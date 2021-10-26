package ujaen.spslidar.services.core;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatablockDTO;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.UTMCoord;
import ujaen.spslidar.repositories.DatablockRepositoryInterface;
import ujaen.spslidar.repositories.FileRepositoryInterface;
import ujaen.spslidar.repositories.mongo.GridFileStorageService;
import ujaen.spslidar.services.tools.LasToolsService;
import ujaen.spslidar.services.tools.SystemFileStorageService;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class DatablockServiceCommonUtils {

    private DatablockRepositoryInterface datablockRepositoryInterface;
    private FileRepositoryInterface fileRepositoryInterface;
    private SystemFileStorageService systemFileStorageService;
    private LasToolsService lasToolsService;

    public DatablockServiceCommonUtils(DatablockRepositoryInterface datablockRepositoryInterface,
                                       FileRepositoryInterface fileRepositoryInterface,
                                       SystemFileStorageService systemFileStorageService,
                                       LasToolsService lasToolsService) {

        this.datablockRepositoryInterface = datablockRepositoryInterface;
        this.fileRepositoryInterface = fileRepositoryInterface;
        this.systemFileStorageService = systemFileStorageService;
        this.lasToolsService = lasToolsService;
    }

    /**
     * Checks if a datablock exists in the database
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @return
     */
    public Mono<Boolean> dataBlockExists(String workspaceName, String datasetName, int id, String southWest, String northEast) {
        GeorefBox grid = stringCoordinatesToGridGeorefBox(southWest, northEast);

        return datablockRepositoryInterface.existsByWorkspaceAndDatasetAndNodeAndGridCell(workspaceName, datasetName, id, grid);
    }

    /**
     * Get metadata of a datablock
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @param southWest
     * @param northEast
     * @return
     */
    public Flux<DatablockDTO> getDatablockData(String workspaceName, String datasetName, int id, String southWest, String northEast) {

        Flux<AbstractDatablock> datablockFlux;

        if (southWest.equals("") || northEast.equals("")) {
            datablockFlux = datablockRepositoryInterface
                    .findDatablockByWorkspaceAndDatasetAndNode(workspaceName, datasetName, id);
        } else {
            GeorefBox grid = stringCoordinatesToGridGeorefBox(southWest, northEast);
            datablockFlux = Flux.from(datablockRepositoryInterface
                    .findDatablockByWorkspaceAndDatasetAndNodeAndGridCell(workspaceName, datasetName, id, grid));
        }

        return datablockFlux.map(DatablockDTO::new);

    }

    public Flux<DataBuffer> getDatablockFile(String workspaceName, String datasetName, int id, String southWest, String northEast) {
        GeorefBox grid = stringCoordinatesToGridGeorefBox(southWest, northEast);

        if (fileRepositoryInterface instanceof GridFileStorageService) {
            Mono<Datablock> datablock = datablockRepositoryInterface
                    .findDatablockByWorkspaceAndDatasetAndNodeAndGridCell(workspaceName, datasetName, id, grid)
                    .cast(Datablock.class);

            return datablock
                    .map(Datablock::getObjectId)
                    .flatMapMany(objectId -> fileRepositoryInterface.getFile(objectId));

        } else {
            return fileRepositoryInterface.getFile(workspaceName, datasetName, id, grid);
        }
    }


    private GeorefBox stringCoordinatesToGridGeorefBox(String southWest, String northEast) {
        UTMCoord sw = UTMCoord.parseUTMCoord(southWest);
        UTMCoord ne = UTMCoord.parseUTMCoord(northEast);
        return new GeorefBox(sw, ne);
    }


    public Mono<Resource> getCompleteDataset(String workspaceName, String datasetName) {

        Path folderToMerge = Path.of(systemFileStorageService.buildMergeDirectory(workspaceName, datasetName));
        Path fileToReturn = folderToMerge.resolve(Path.of("merged.laz"));

        return datablockRepositoryInterface.findAllDatablocksInDataset(workspaceName, datasetName)
                .cast(Datablock.class)
                .map(Datablock::getObjectId)
                .flatMap(objectId -> {
                    Path resourceFileName = folderToMerge.resolve(Path.of(objectId.toString() + ".laz"));

                    try {
                        AsynchronousFileChannel asynchronousFileChannel =
                                AsynchronousFileChannel.open(resourceFileName, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                        return DataBufferUtils.write(fileRepositoryInterface.getFile(objectId), asynchronousFileChannel)
                                .map(DataBufferUtils::release)
                                .then(Mono.just(String.valueOf(resourceFileName)));
                    } catch (IOException ioException) {
                        return Flux.error(new RuntimeException("Couldn't merge files"));
                    }

                })
                .collectList()
                .flatMap(strings -> lasToolsService.mergeFilesReturn(strings, fileToReturn)
                        .map(path -> {
                            Resource resource = new FileSystemResource(path);
                            return resource;
                        }))
                .doAfterTerminate(() -> systemFileStorageService.cleanDirectory(folderToMerge));


    }

}