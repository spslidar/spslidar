package ujaen.spslidar.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.GeorefBox;

/**
 * Repository interface for the datablock entity
 */
public interface DatablockRepositoryInterface {

    /**
     * Query for the datablocks metadata
     * @param workspace workspace name
     * @param dataset dataset name
     * @param node id of the node
     * @return Flux of datablocks
     */
    Flux<AbstractDatablock> findDatablockByWorkspaceAndDatasetAndNode(String workspace, String dataset, int node);

    /**
     * Check existance of a datablock
     * @param workspace workspace name
     * @param dataset dataset name
     * @param node id of the node
     * @param grid grid cell in which the node is located
     * @return Mono with the result boolean
     */
    Mono<Boolean> existsByWorkspaceAndDatasetAndNodeAndGridCell(String workspace, String dataset, int node, GeorefBox grid);

    /**
     * Query for a specific datablock
     * @param workspace workspace name
     * @param dataset dataset name
     * @param node id of the node
     * @param grid grid cell in which the node is located
     * @return Mono with the datablock if it exists, otherwise Mono empty
     */
    Mono<AbstractDatablock> findDatablockByWorkspaceAndDatasetAndNodeAndGridCell(String workspace, String dataset, int node, GeorefBox grid);

    /**
     * Save a datablock in the system
     * @param abstractDatablock metadata of the datablock
     * @param workspace workspace to which it belongs to
     * @return Mono with the datablock inserted
     */
    Mono<AbstractDatablock> save(AbstractDatablock abstractDatablock, String workspace, String datasetName);

    /**
     * Find all the datablocks in a dataset.
     * @param workspace workspace name
     * @param dataset dataset name
     * @return Flux of datablocks
     */
    Flux<AbstractDatablock> findAllDatablocksInDataset(String workspace, String dataset);



}
