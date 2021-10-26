package ujaen.spslidar.services.tools;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.Exceptions.NoUTMZoneInFile;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.utils.properties.LasToolsProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LasToolsService {

    Logger logger = LoggerFactory.getLogger(LasToolsService.class);

    private String LAZ_EXTENSION;
    private static final String SPLIT_FILENAME = "_";
    private static final String BASE_EXT = "base";
    private static final String CHANGE_USER_DATA_EXT = "udmod";
    private static final String BD_READY = "bd";
    private static final String MERGED_EXT = "merged";
    private static final String PARTITION_READY = "pready";
    private static final String ROOT_EXT = "root";
    private static final String FILE_EXT_BD = ".laz";
    private static final String OPTIMIZED = "optimized";
    private static final double nodeMargin = 0.01;

    @Autowired
    @Qualifier("lazReaderServicePylasImplementation")
    LazReaderInterface lazReaderInterface;

    @Autowired
    SystemFileStorageService systemFileStorageService;

    public LasToolsService(LasToolsProperties lasToolsProperties) {
        LAZ_EXTENSION = lasToolsProperties.getExtension();
    }


    /**
     * Receives a datablock and a value, changing the user_id of the file associated to this datablock
     * to the value specified
     *
     * @param datablock
     * @param value
     * @return the input datablock with a new tmpOpsFileAssociated
     */
    public Mono<Datablock> changeUserData(Datablock datablock, int value) {


        String outputFile = composeOutputFileName(datablock.getTmpOpsFile(),
                CHANGE_USER_DATA_EXT + value, Optional.empty());

        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i",
                datablock.getTmpOpsFile(), "-set_user_data",
                String.valueOf(value), "-o",
                outputFile);

        logger.debug("Changing user data->" + outputFile);

        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock).map(dblock -> {
                    dblock.setTmpOpsFile(outputFile);
                    return dblock;
                }));

    }

    /**
     * Create a new file which will contain the sampled points wanted for a particular node.
     * The datablock passed will have its lazFileAssociated attribute set to this one file,
     * as it is the one we want to store in the end.
     *
     * @param datablock
     * @param maxDataBlockSize
     * @return
     */
    public Mono<Datablock> sampleDataFromFile(Datablock datablock, int maxDataBlockSize) {

        String outputFile = composeOutputFileName(datablock.getTmpOpsFile(), BD_READY, Optional.empty());

        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i", datablock.getTmpOpsFile(),
                "-o", outputFile,
                "-keep_random_fraction", String.valueOf(samplePercentageDeterminer(datablock, maxDataBlockSize)));

        logger.debug("Sampling data->" + outputFile);
        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock).map(dblock -> {
                    dblock.setLazFileAssociated(outputFile);
                    dblock.setTmpOpsFile(outputFile);
                    return dblock;
                }));

    }


    /**
     * Generates a file with the sampled data and one with the removed data previously sampled
     * by using keep_every_nth and drop_every_nth options from las2las
     *
     * @param datablock
     * @param maxDataBlockSize
     * @return
     */
    public Mono<Datablock> sampleDataFromFileWithKeepNth(Datablock datablock, int maxDataBlockSize) {

        String tmpOpsFile = datablock.getTmpOpsFile();
        String lazFileAssociated = composeOutputFileName(tmpOpsFile, BD_READY, Optional.empty());

        return stepNthDeterminer(datablock, maxDataBlockSize)
                .flatMap(nthStepValue -> {
                    if (nthStepValue == 1) {
                        try {
                            Files.copy(Path.of(tmpOpsFile), Path.of(lazFileAssociated));
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }

                        datablock.setLazFileAssociated(lazFileAssociated);
                        datablock.setTmpOpsFile("");
                        return Mono.just(datablock);
                    } else {
                        ProcessBuilder processBuilderKeep = new ProcessBuilder("las2las", "-i",
                                tmpOpsFile, "-o", lazFileAssociated,
                                "-keep_every_nth", String.valueOf(nthStepValue));

                        String partitionFileAssociated = composeOutputFileName(tmpOpsFile, PARTITION_READY, Optional.empty());

                        ProcessBuilder processBuilderDrop = new ProcessBuilder("las2las", "-i",
                                tmpOpsFile, "-o", partitionFileAssociated,
                                "-drop_every_nth", String.valueOf(nthStepValue));

                        logger.debug("Sampling data->" + lazFileAssociated);

                        return processBuilderRunner(processBuilderKeep)
                                .zipWith(processBuilderRunner(processBuilderDrop))
                                .then(Mono.just(datablock)
                                        .doOnSuccess(dblock -> systemFileStorageService.deleteFiles(datablock.getTmpOpsFile()))
                                        .map(dblock -> {
                                            dblock.setLazFileAssociated(lazFileAssociated);
                                            dblock.setTmpOpsFile(partitionFileAssociated);
                                            return dblock;
                                        }));
                    }
                });
    }


    /**
     * Through lasduplicate and a merge operation, we get a new file that has the points from the
     * parent file and the points of the node, but these last ones have a unique user data that
     * we can filter in a future operation
     *
     * @param datablock
     * @param inputParentFile
     * @return
     */
    public Mono<Datablock> mergeWithDuplicates(Datablock datablock, String inputParentFile) {


        String outputFile = composeOutputFileName(inputParentFile, MERGED_EXT, Optional.empty());

        ProcessBuilder processBuilder = new ProcessBuilder("lasduplicate", "-i",
                datablock.getTmpOpsFile(), inputParentFile, "-merged", "-unique_xyz", "-o", outputFile);

        logger.debug("Merge with duplicates, inputs ->" + datablock.getTmpOpsFile() + " and " + inputParentFile + " , output->" + outputFile);
        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock).map(dblock -> {
                    dblock.setTmpOpsFile(outputFile);
                    return dblock;
                }));
    }

    /**
     * Extract the duplicate values, obtaining as a result a new file that is the substraction
     * of the points of the original file minus the points of the node.
     *
     * @param datablock
     * @param value
     * @return
     */
    public Mono<Datablock> extractDuplicates(Datablock datablock, int value) {

        String outputFile = composeOutputFileName(datablock.getTmpOpsFile(), PARTITION_READY, Optional.empty());

        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i", datablock.getTmpOpsFile(),
                "-keep_user_data", String.valueOf(value), "-o", outputFile);

        logger.debug("Extracting duplicates->" + outputFile);
        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock).map(dblock -> {
                    //deleteFile(dblock.getTmpOpsFile());
                    dblock.setTmpOpsFile(outputFile);
                    return dblock;
                }));

    }


    /**
     * Creates a file with the points of a specific region, determined by the bounding
     * box of the childDataBlock
     *
     * @param parentFile
     * @param childDataBlock
     * @return
     */
    public Mono<Datablock> createChildNode(String parentFile, Datablock childDataBlock) {


        String outputFile = composeOutputFileName(parentFile, BASE_EXT, Optional.of(childDataBlock.getId()));

        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i",
                parentFile, "-o", outputFile, "-keep_xyz",
                String.valueOf(childDataBlock.getGeorefBox().getSouthWestBottom().getEasting()),
                String.valueOf(childDataBlock.getGeorefBox().getSouthWestBottom().getNorthing()),
                String.valueOf(childDataBlock.getGeorefBox().getSouthWestBottom().getHeight()),
                String.valueOf(childDataBlock.getGeorefBox().getNorthEastTop().getEasting() + nodeMargin),
                String.valueOf(childDataBlock.getGeorefBox().getNorthEastTop().getNorthing() + nodeMargin),
                String.valueOf(childDataBlock.getGeorefBox().getNorthEastTop().getHeight()) + nodeMargin);

        logger.debug("Creating children->" + outputFile);
        return processBuilderRunner(processBuilder)
                .then(Mono.just(childDataBlock).map(dblock -> {
                    dblock.setTmpOpsFile(outputFile);
                    return dblock;
                }));

    }


    /**
     * Creates the root file that will serve to build the octree for a specific grid and dataset
     *
     * @param inputDirectory  directory of the parent file
     * @param outputDirectory directory where the output file will be located
     * @param dataset         dataset associated to the root file that will be created
     * @param georefBox       corresponding an specific grid inside UTMCell in which the original bounding box of the dataset overlaps
     * @return Mono of the outputfile
     */
    public Mono<String> createRootFile(String inputDirectory, String outputDirectory, Dataset dataset, GeorefBox georefBox) {

        String parentFile = inputDirectory + "\\" + MERGED_EXT + LAZ_EXTENSION;
        String outputFile = outputDirectory + "\\" + dataset.getWorkspaceName() + "_" + dataset.getDatasetName() + "_0_" + ROOT_EXT + LAZ_EXTENSION;


        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i",
                parentFile, "-o", outputFile, "-keep_xy",
                String.valueOf(georefBox.getSouthWestBottom().getEasting()),
                String.valueOf(georefBox.getSouthWestBottom().getNorthing()),
                String.valueOf(georefBox.getNorthEastTop().getEasting()),
                String.valueOf(georefBox.getNorthEastTop().getNorthing()));

        logger.info("Creating root file->" + outputFile);

        return processBuilderRunner(processBuilder)
                .flatMap(o -> {
                    //In some cases, a file will not be created, as the merged files in which we are
                    //basing the local grids to initiate gave a false positive.
                    if (!Files.exists(Path.of(outputFile))) {
                        logger.info("No points found in this grid. Detected as a false positive");
                        return Mono.empty();
                    } else {
                        return Mono.just(outputFile);
                    }
                });

    }


    /**
     * Merges a list of files and creates a new file with the result
     *
     * @param filesTomerge
     * @param path
     * @return
     */
    public Mono<String> mergeFiles(List<String> filesTomerge, String path) {

        String outputFile = path + "\\merged" + LAZ_EXTENSION;

        List<String> arguments = new ArrayList<>();
        arguments.add("lasmerge");
        arguments.add("-i");
        arguments.addAll(filesTomerge);
        arguments.add("-o");
        arguments.add(outputFile);

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);

        return processBuilderRunner(processBuilder)
                .then(Mono.just(outputFile));

    }

    /**
     * Merges a list of files and creates a new file with the result
     *
     * @param filesTomerge
     * @param fileToReturn
     * @return
     */
    public Mono<Path> mergeFilesReturn(List<String> filesTomerge,
                                       Path fileToReturn) {


        List<String> arguments = new ArrayList<>();
        arguments.add("lasmerge");
        arguments.add("-i");
        arguments.addAll(filesTomerge);
        arguments.add("-o");
        arguments.add(fileToReturn.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);

        return processBuilderRunner(processBuilder)
                .then(Mono.just(fileToReturn));
    }


    public Mono<Datablock> convertMaxDepthFileToBDReady(Datablock datablock) {

        String tmpOpsFile = datablock.getTmpOpsFile();
        String lazFileAssociated = composeOutputFileName(tmpOpsFile, BD_READY, Optional.empty());

        ProcessBuilder processBuilder = new ProcessBuilder("las2las", "-i",
                tmpOpsFile, "-o", lazFileAssociated);

        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock))
                .map(dblock -> {
                    dblock.setLazFileAssociated(lazFileAssociated);
                    return dblock;
                });


    }

    public Mono<Datablock> optimizeFile(Datablock datablock) {

        String lazFileAssociated = composeOutputFileName(datablock.getLazFileAssociated(), OPTIMIZED, Optional.empty());
        ProcessBuilder processBuilder = new ProcessBuilder("lasoptimize", "-i", datablock.getLazFileAssociated(),
                "-o", lazFileAssociated);

        return processBuilderRunner(processBuilder)
                .then(Mono.just(datablock))
                .map(dblock -> {
                    dblock.setLazFileAssociated(lazFileAssociated);
                    return dblock;
                });


    }


    /**
     * Simple method that retrieves the root file. No LasTool operation involved but
     * as it needs to get the extension used, it has been brought here for decoupling
     *
     * @param outputDirectory
     * @param dataset
     * @return
     */
    public String declareRootFile(String outputDirectory, Dataset dataset) {

        return outputDirectory + "\\" + dataset.getWorkspaceName() + "_" + dataset.getDatasetName() + "_0_" + ROOT_EXT + LAZ_EXTENSION;

    }


    /**
     * Retrieves the UTM Zone of a file
     *
     * @param file
     * @return
     */
    public Mono<String> getUTMZone(String file) {

        Pattern UTMPattern = Pattern.compile("\\d{2}[A-Z]{1}");

        return getInfoFromFile(file)
                .flatMap(strings -> Flux.fromIterable(strings)
                        .filter(s -> s.contains("UTM"))
                        .flatMap(s -> Flux.fromArray(s.split(" "))
                                .filter(substring -> substring.matches(UTMPattern.pattern()))
                        ).collectList()
                )
                .map(strings -> {
                    if (strings.isEmpty()) {
                        throw new NoUTMZoneInFile();
                    } else {
                        return strings.get(0);
                    }
                });
    }


    /**
     * Uses lasinfo to get the information available of the file provided by this tool
     *
     * @param file
     * @return
     */
    private Mono<List<String>> getInfoFromFile(String file) {

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("lasinfo", file, "-no_check");

        //Info is actually generated in the error stream so we need to redirect it
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            List<String> inputStream = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.toList());
            return Mono.just(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
            return Mono.error(e);
        }


    }


    /**
     * Method to create the files with a significative name for each step.
     *
     * @param inputFileName
     * @param extension
     * @return
     */
    private String composeOutputFileName(String inputFileName, String extension, Optional<Integer> node) {

        String[] fileNameParts = FilenameUtils.getBaseName(inputFileName).split(SPLIT_FILENAME);

        String outputFileBaseName = fileNameParts[0] + "_"
                + fileNameParts[1]
                + "_" + ((node.isPresent()) ? node.get() : fileNameParts[2])
                + "_" + extension;

        String fileName = FilenameUtils.getFullPath(inputFileName) + outputFileBaseName;

        return (extension == BD_READY || extension == OPTIMIZED ? fileName + FILE_EXT_BD : fileName + LAZ_EXTENSION);
    }


    /**
     * Returns the percentage value that should be specified in the call to LasTools
     * Also updates the attribute number of points of the corresponding datablock
     *
     * @param maxDataBlockSize
     * @param datablock
     * @return
     */
    private Mono<Double> samplePercentageDeterminer(Datablock datablock, int maxDataBlockSize) {

        return lazReaderInterface.getNumberOfPoints(datablock.getTmpOpsFile())
                .map(numberOfPoints -> {
                    double percentage = Double.valueOf(maxDataBlockSize) / Double.valueOf(numberOfPoints);
                    long dblockNumberOfPoints = ((percentage <= 1) ? maxDataBlockSize : numberOfPoints);
                    datablock.setNumberOfPoints(dblockNumberOfPoints);
                    return percentage;
                });

    }


    private Mono<Integer> stepNthDeterminer(Datablock datablock, int maxDataBlockSize) {

        return lazReaderInterface.getNumberOfPoints(datablock.getTmpOpsFile())
                .map(numberOfPoints -> {
                    int step = (int) Math.floorDiv(numberOfPoints, maxDataBlockSize) + 1;
                    //Round down to avoid surpassing the maxDatablockSize
                    datablock.setNumberOfPoints(Math.floorDiv(numberOfPoints, step));

                    return step;
                });
    }


    /**
     * Method that manages the execution of a process call
     *
     * @param processBuilder
     * @return
     */
    private Mono<Object> processBuilderRunner(ProcessBuilder processBuilder) {

        return Mono.fromCallable(() -> {
            Process process = processBuilder.start();
            return process.waitFor();
        });
    }
}
