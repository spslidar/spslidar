package ujaen.spslidar.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.GridCell;

/**
 * Repository interface for the grid cells
 */
public interface GridCellRepositoryInterface {

    /**
     * Query for a cell that has the bounding box passed
     * @param workspaceName name of the workspace
     * @param box bounding box passed
     * @return Mono with a gridCell if exists or Mono empty
     */
    Mono<GridCell> findById(String workspaceName, GeorefBox box);

    /**
     * Query the datasets associated to a specific grid cell
     * @param workspaceName name of the workspace
     * @param gridCell grid cell to query
     * @return Flux of the name of the models associated to a specific grid cell
     */
    Flux<String> findDatasetsByGrid(String workspaceName, GridCell gridCell);

    /**
     * Query the datasets associated to the grids that intersect with the passed georefbox
     * @param workspaceName name of the workspace
     * @param box geowindow to filter
     * @return Flux of the name of the models associated to a specific grid cell
     */
    Flux<String> findDatasetsByGeorefBox(String workspaceName, GeorefBox box);

    /**
     * Save new grid in the system
     * @param workspaceName name of the workspace
     * @param gridCell grid cell to save
     * @return Metadata of the grid cell to insert
     */
    Mono<GridCell> save(String workspaceName, GridCell gridCell);


}
