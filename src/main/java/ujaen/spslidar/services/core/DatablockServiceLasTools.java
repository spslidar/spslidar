package ujaen.spslidar.services.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import ujaen.spslidar.DTOs.http.DatablockDTO;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.repositories.DatablockRepositoryInterface;
import ujaen.spslidar.repositories.DatasetRepositoryInterface;
import ujaen.spslidar.repositories.FileRepositoryInterface;
import ujaen.spslidar.repositories.mongo.DatablockRepositoryMongo;
import ujaen.spslidar.repositories.mongo.IndexManagerMongo;
import ujaen.spslidar.services.core.algorithms.LasToolsAlgorithmInterface;
import ujaen.spslidar.services.tools.LasToolsService;
import ujaen.spslidar.services.tools.LazReaderInterface;
import ujaen.spslidar.services.tools.SystemFileStorageService;
import ujaen.spslidar.utils.NodeSizeDistribution;
import ujaen.spslidar.utils.properties.OctreeProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Dataservice that uses Las Tools in order
 * to build the octree representation of the dataset
 */
@Service
public class DatablockServiceLasTools implements DatablockService {

    //Logger
    Logger logger = LoggerFactory.getLogger(DatablockServiceLasTools.class);

    //Repositories
    DatasetRepositoryInterface datasetRepositoryInterface;
    DatablockRepositoryInterface datablockRepositoryInterface;
    IndexManagerMongo indexManagerMongo;

    //Auxiliar services
    SystemFileStorageService systemFileStorageService;
    LasToolsService lasToolsService;
    FileRepositoryInterface fileRepositoryInterface;
    DatablockServiceCommonUtils datablockServiceCommonUtils;
    LazReaderInterface lazReaderInterface;
    NodeSizeDistribution nodeSizeDistribution;
    LasToolsAlgorithmInterface lasToolsAlgorithmInterface;

    //Properties
    OctreeProperties octreeProperties;


    public DatablockServiceLasTools(LasToolsService lasToolsService,
                                    @Qualifier("gridFileStorageService") FileRepositoryInterface fileRepositoryInterface,
                                    @Qualifier("datasetRepositoryMongo") DatasetRepositoryInterface datasetRepositoryInterface,
                                    SystemFileStorageService systemFileStorageService,
                                    @Qualifier("datablockRepositoryMongo") DatablockRepositoryInterface datablockRepositoryInterface,
                                    IndexManagerMongo indexManagerMongo,
                                    @Qualifier("lazReaderServicePylasImplementation") LazReaderInterface lazReaderInterface,
                                    DatablockServiceCommonUtils datablockServiceCommonUtils,
                                    OctreeProperties octreeProperties,
                                    NodeSizeDistribution nodeSizeDistribution,
                                    @Qualifier("fastRecursiveOctreeBuilding") LasToolsAlgorithmInterface lasToolsAlgorithmInterface) {

        this.lasToolsService = lasToolsService;
        this.fileRepositoryInterface = fileRepositoryInterface;
        this.datasetRepositoryInterface = datasetRepositoryInterface;
        this.systemFileStorageService = systemFileStorageService;
        this.datablockRepositoryInterface = datablockRepositoryInterface;
        this.lazReaderInterface = lazReaderInterface;
        this.datablockServiceCommonUtils = datablockServiceCommonUtils;
        this.octreeProperties = octreeProperties;
        this.nodeSizeDistribution = nodeSizeDistribution;
        this.lasToolsAlgorithmInterface = lasToolsAlgorithmInterface;
        this.indexManagerMongo = indexManagerMongo;
    }


    @Override
    public Flux<DatablockDTO> getDatablockData(String workspaceName, String datasetName, int id, String southWest, String northEast) {

        return datablockServiceCommonUtils
                .getDatablockData(workspaceName, datasetName, id, southWest, northEast);
    }

    @Override
    public Flux<DataBuffer> getDatablockFile(String workspaceName, String datasetName, int id, String southWest, String northEast) {
        return datablockServiceCommonUtils.getDatablockFile(workspaceName, datasetName, id, southWest, northEast);
    }

    @Override
    public Mono<Boolean> addDataToDataset(String workspaceName, String datasetName,
                                          Flux<FilePart> files) {

        logger.info("Adding the data recieved to the dataset started");

        Mono<Dataset> m = datasetRepositoryInterface.findByWorkspaceAndDataset(workspaceName, datasetName);
        Mono<List<String>> filePath = systemFileStorageService.storeMultipleFiles(files, workspaceName, datasetName).collectList();


        return Mono.zip(m, filePath)
                .flatMap(t -> divideDatasetByUTMCells(workspaceName, datasetName, t.getT2())
                        .collectList()
                        .map(list -> {
                            t.getT1().setFiles(list);
                            return t.getT1();
                        }))
                .flatMapMany(dataset -> {
                    logger.info("Octree building phase");
                    Flux<String> fluxUTMZones = Flux.fromIterable(dataset.getRootDatablocks().keySet());
                    Flux<Datablock> datablockFlux = fluxUTMZones.flatMap((UTMZone -> {
                        List<GeorefBox> georefBoxList = dataset.getRootDatablocks().get(UTMZone);
                        return Flux.fromIterable(georefBoxList)
                                .parallel()
                                .runOn(Schedulers.boundedElastic())
                                .flatMap(UTMLocalGrid -> {
                                    Mono<String> outputFile = createRootFileOfGrid(UTMLocalGrid, dataset, UTMZone);
                                    int datablockSize = dataset.getDataBlockSize();
                                    return outputFile.flatMapMany(file -> octreeBuilding(file, lazReaderInterface.getGeorefBox(file), datablockSize, UTMLocalGrid));
                                });
                    }));

                    Mono<Dataset> monoDataset = datasetRepositoryInterface.update(dataset);
                    return storeOctree(monoDataset, datablockFlux, workspaceName);
                })
                .last()
                .doOnError(throwable -> {
                    systemFileStorageService.cleanDirectory(workspaceName, datasetName);
                })
                .doOnNext(datablockDBDTO -> {
                    systemFileStorageService.cleanDirectory(workspaceName, datasetName);
                    logger.info("Finished insertion process for " + workspaceName + " - " + datasetName);
                })
                .thenReturn(Boolean.TRUE)
                .log();


    }

    @Override
    public Mono<Resource> getCompleteDataset(String workspaceName, String datasetName) {

        return datablockServiceCommonUtils.getCompleteDataset(workspaceName, datasetName);
    }


    /**
     * Groups the files associated to a dataset by the UTM zone they are located in and merges them, obtaining as a result
     * new files that contain all the data associated to the dataset divided by the UTM zone
     *
     * @param workspaceName name of the workspace
     * @param datasetName   name of the dataset
     * @param files         list of files associated originally to the dataset
     * @return a Mono of a List of Strings that reference the new files created
     */
    private Flux<String> divideDatasetByUTMCells(String workspaceName, String datasetName, List<String> files) {

        return Flux.fromIterable(files)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(file -> Mono.zip(lasToolsService.getUTMZone(file), Mono.just(file)))
                .doOnNext(objects -> {
                    logger.info("File: " + objects.getT2() + " with UTM Zone " + objects.getT1());
                })
                .sequential()
                .groupBy(Tuple2::getT1)
                .flatMap(UTMZoneGroupedFlux -> {
                    String key = (String) UTMZoneGroupedFlux.key();
                    String appendDirectory = "\\" + workspaceName + "_" + datasetName + "\\" + key;
                    String targetDirectoryOfMergedFile = systemFileStorageService.buildDirectory(appendDirectory);

                    return UTMZoneGroupedFlux.collectList()
                            .flatMap(tuple2s -> {
                                List<String> filesToMerge = tuple2s.stream().map(Tuple2::getT2).collect(Collectors.toList());
                                return lasToolsService.mergeFiles(filesToMerge, targetDirectoryOfMergedFile);
                            });
                }).doOnComplete(() -> systemFileStorageService.deleteFiles(files));
    }

    /**
     * Creates the "root file" of a grid, this meaning the base file for a specific combination of workspace - dataset - UTMZone - inner UTM
     * zone grid from which the octree building process will start.
     *
     * @param grid    a georef box that corresponds to an arbitrary grid partition of the world, specifically for the UTM zone determined
     * @param dataset dataset the data belongs to
     * @param UTMZone UTMZone in which this specific grid is located
     * @return a String in a Mono with the location of the created file or a Mono.empty() in case
     * no file was created
     */
    private Mono<String> createRootFileOfGrid(GeorefBox grid, Dataset dataset, String UTMZone) {

        String datasetFolder = "\\" + dataset.getWorkspaceName() + "_" + dataset.getDatasetName() + "\\";
        String georefIdentifier = grid.georefBox2DIdentifier();
        String appendDirectory = datasetFolder + UTMZone + "\\" + georefIdentifier;
        String outputDirectory = systemFileStorageService.buildDirectory(appendDirectory);
        String inputDirectory = systemFileStorageService.getBasePath() + datasetFolder + UTMZone;
        return lasToolsService.createRootFile(inputDirectory, outputDirectory, dataset, grid);

    }


    /**
     * Manages the creation of the files and corresponding datablocks for an input file
     * using an octree as reference
     *
     * @param inputFileName single file name, if the dataset contains multiple files they have to be merged
     * @param georefBoxArg  corresponding to the region delimited by the input file or one modified by the
     *                      user (for example, a cubic georefbox for a rectangular space)
     * @param dataBlockSize
     * @param UTMLocalGrid
     * @return
     */
    private Flux<Datablock> octreeBuilding(String inputFileName, Mono<GeorefBox> georefBoxArg, int dataBlockSize, GeorefBox UTMLocalGrid) {
        logger.info("Bulding octree");

        return georefBoxArg.flatMapMany(georefBox -> {
            if (!georefBox.getSouthWestBottom().getZone().equals(georefBox.getNorthEastTop().getZone())) {
                return Flux.error(new RuntimeException());
            }
            if (!Files.exists(Path.of(inputFileName))) {
                return Flux.empty();
            }
            logger.info("Original base georefbox is: " + georefBox.toString());

            Datablock rootDatablock = new Datablock(0, georefBox, georefBox.getSouthWestBottom().getZone(), UTMLocalGrid);
            rootDatablock.setTmpOpsFile(inputFileName);

            if (octreeProperties.isLinealDistribution()) {
                Mono<List<Integer>> sizes = lazReaderInterface.getNumberOfPoints(inputFileName)
                        .map(aLong -> nodeSizeDistribution.percentagesGenerator(aLong));

                return sizes.flatMapMany(longs ->
                        lasToolsAlgorithmInterface.octreeBuildingWithDistribution(rootDatablock, longs));

            } else {
                return lasToolsAlgorithmInterface.octreeBuildingAlgorithm(rootDatablock, dataBlockSize);
            }
        });
    }


    /**
     * Store each of the datablocks defined by calling the gridFileStorageService
     *
     * @param dataset
     * @param datablockFlux
     * @return
     */
    private Flux<AbstractDatablock> storeOctree(Mono<Dataset> dataset,
                                                Flux<Datablock> datablockFlux,
                                                String workspaceName) {
        logger.info("Octree storing phase");

        //Particular case: if we are using Mongo, we need to create a secondary index for this
        //particular collection to speed future queries
        if (datablockRepositoryInterface instanceof DatablockRepositoryMongo)
            indexManagerMongo.createIndex(workspaceName).subscribe();


        return dataset.flatMapMany(dataset1 -> datablockFlux
                .publishOn(Schedulers.newBoundedElastic(10, 24, "storeOctreeThread"))
                .flatMap(lasToolsService::optimizeFile)
                .flatMap(datablock -> fileRepositoryInterface.addFile(datablock, dataset1)) //Store the file
                .flatMap(datablock -> datablockRepositoryInterface.save(datablock, workspaceName, dataset1.getDatasetName()))
                .doOnComplete(() -> System.out.println("Finished storing data"))//Store the datablock metadata
        );

    }


}
