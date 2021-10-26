package ujaen.spslidar.repositories;

import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.Dataset;

/**
 * Repository interface for the files
 */
public interface FileRepositoryInterface {

    /**
     * Get file by the object ID associated to a datablock
     * @param objectId id of the file stored in gridFS
     * @return Flux of DataBuffer with the content of the file
     */
    Flux<DataBuffer> getFile(ObjectId objectId);

    /**
     * Get file associated to a particular workspace-dataset-grid cell-node
     * @param workspaceName name of the workspace
     * @param datasetName name of the dataset
     * @param node id of the node
     * @param box box of the grid cell
     * @return Flux of DataBuffer with the content of the file
     */
    Flux<DataBuffer> getFile(String workspaceName, String datasetName, int node, GeorefBox box);

    /**
     * Add a new file to the system
     * @param datablock datablock associated to the file
     * @param dataset dataset associated to the datablock
     * @return Datablock metadata
     */
    Mono<AbstractDatablock> addFile(AbstractDatablock datablock, Dataset dataset);


}
