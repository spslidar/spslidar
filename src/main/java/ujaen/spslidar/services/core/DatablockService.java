package ujaen.spslidar.services.core;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatablockDTO;

public interface DatablockService {

    /**
     * Returns the metadata corresponding to a datablock
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @return
     */
    Flux<DatablockDTO> getDatablockData(String workspaceName, String datasetName, int id, String southWest, String northEast);

    /**
     * Returns the file corresponding to a datablock
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @return
     */
    Flux<DataBuffer> getDatablockFile(String workspaceName, String datasetName, int id, String southWest, String northEast);


    /**
     * Creates the datablocks associated to some initial files and associates them to
     * an already existing dataset
     *
     * @param workspaceName
     * @param datasetName
     * @param files
     * @return
     */
    Mono<Boolean> addDataToDataset(String workspaceName, String datasetName, Flux<FilePart> files);


    /**
     * Returns the complete dataset, either merged in one single file or maintaining the
     * file structure defined in the storage of the system
     *
     * @param workspaceName
     * @param datasetName
     * @return
     */
    Mono<Resource> getCompleteDataset(String workspaceName, String datasetName);

}
