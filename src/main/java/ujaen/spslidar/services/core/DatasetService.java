/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.services.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatasetDTO;
import ujaen.spslidar.entities.*;
import ujaen.spslidar.repositories.DatasetRepositoryInterface;
import ujaen.spslidar.repositories.GridCellRepositoryInterface;
import ujaen.spslidar.repositories.WorkspaceRepositoryInterface;
import ujaen.spslidar.utils.geoconverter.GeoConverter;
import ujaen.spslidar.utils.geoconverter.GridAllocator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author l3pc126
 */
@Service
public class DatasetService {

    private WorkspaceRepositoryInterface workspaceRepositoryInterface;
    private DatasetRepositoryInterface datasetRepositoryInterface;
    private GridCellRepositoryInterface gridCellRepositoryInterface;

    public DatasetService(WorkspaceRepositoryInterface workspaceRepositoryInterface,
            DatasetRepositoryInterface datasetRepositoryInterface,
            GridCellRepositoryInterface gridCellRepositoryInterface) {

        this.workspaceRepositoryInterface = workspaceRepositoryInterface;
        this.datasetRepositoryInterface = datasetRepositoryInterface;
        this.gridCellRepositoryInterface = gridCellRepositoryInterface;
    }

    Logger logger = LoggerFactory.getLogger(DatasetService.class);


    /**
     * Finds the datasets associated to a workspace, filtering optionally by coordinates
     *
     * @param workspaceName
     * @param southWest     southwest coordinate of the bounding box used to filter. If this value does not exist
     *                      but a northEast coordinate was provided, a default value will be assigned, equal to the minimum value possible.
     * @param northEast     northeast coordinate of the bounding box used to filter. If this value does not exist
     *                      but a southWest coordinate was provided, a default value will be assigned, equal to the maximum value possible.
     * @return Flux of datasets
     */

    public Flux<DatasetDTO> getDatasets(String workspaceName,
                                        String southWest, String northEast,
                                        LocalDateTime fromDate, LocalDateTime toDate) {

        logger.info("Getting datasets of the workspace");

        //Case 1: No bounding box specified. Filter only by time window
        //Time window will get default values so if no window was provided, will use MIN - MAX dates
        if (southWest.isEmpty() && northEast.isEmpty()) {
            return datasetRepositoryInterface
                    .findByWorskpaceNameAndUTMZoneAndTimeWindow(workspaceName, "", fromDate, toDate)
                    .map(DatasetDTO::new);
        }

        //Case 2: Bounding box specified
        //Parse UTMCoords from Strings
        UTMCoord UTMCoordSouthWest = southWest.isEmpty() ? UTMCoord.defaultSouthWest() : UTMCoord.parseUTMCoord(southWest);
        UTMCoord UTMCoordNorthEast = northEast.isEmpty() ? UTMCoord.defaultNorthEast() : UTMCoord.parseUTMCoord(northEast);

        //Case 2.1: Bounding box extends to 2 UTM zones
        //Different UTM zones, convert to Longitude-Latitude to filter the correct datasets
        if (!UTMCoordNorthEast.getZone().equals(UTMCoordSouthWest.getZone())) {

            //Compose two bounding box, each one in a different UTM zone.
            //Find datasets that adjust to them and remove duplicates
            UTMCoord UTMCoordNorthEastReprojected = GeoConverter.reprojectUTMCoordinate(UTMCoordNorthEast, UTMCoordSouthWest.getZone());
            GeorefBox georefBoxWithCoordInSWUTMZone = new GeorefBox(UTMCoordSouthWest, UTMCoordNorthEastReprojected);

            UTMCoord UTMCoordSouthWestReprojected = GeoConverter.reprojectUTMCoordinate(UTMCoordSouthWest, UTMCoordNorthEast.getZone());
            GeorefBox georefBoxWithCoordInNEUTMZone = new GeorefBox(UTMCoordSouthWestReprojected, UTMCoordNorthEast);

            Flux<String> datasetsInBboxSWUTMZone = gridCellRepositoryInterface.findDatasetsByGeorefBox(workspaceName, georefBoxWithCoordInSWUTMZone);
            Flux<String> datasetsInBboxNEUTMZone = gridCellRepositoryInterface.findDatasetsByGeorefBox(workspaceName, georefBoxWithCoordInNEUTMZone);

            return datasetsInBboxSWUTMZone.concatWith(datasetsInBboxNEUTMZone)
                    .distinct()
                    .flatMap(datasetName -> datasetRepositoryInterface.findByWorkspaceAndDatasetAndTimeWindow(workspaceName, datasetName, fromDate, toDate))
                    .map(DatasetDTO::new);

        } else {
            //Case 2.2: Query bounding box in a single UTM zone
            GeorefBox georefBoxQuery = new GeorefBox(UTMCoordSouthWest, UTMCoordNorthEast);

            return gridCellRepositoryInterface.findDatasetsByGeorefBox(workspaceName, georefBoxQuery)
                    .distinct()
                    .flatMap(datasetName -> datasetRepositoryInterface.findByWorkspaceAndDatasetAndTimeWindow(workspaceName, datasetName, fromDate, toDate))
                    .map(DatasetDTO::new);

        }

    }

    /**
     * Adds a new dataset to the system, only containing the workspace it belongs to and its name.
     * It will have to be edited at a later time in order to associate it to a Laz file and instantiate its Georefbox
     *
     * @param workspaceName name of the workspace
     * @param datasetDTO    dataset to be introduced, contains its name, workspace name and description
     * @return the dataset that was created
     */
    @Transactional
    public Mono<DatasetDTO> addDataset(String workspaceName, DatasetDTO datasetDTO) {

        Dataset dataset = new Dataset(datasetDTO);
        String zoneSouthWestBottom = dataset.getBbox().getSouthWestBottom().getZone();
        String zoneNorthEastTop = dataset.getBbox().getNorthEastTop().getZone();

        Mono<Workspace> workspaceMono = workspaceRepositoryInterface.findByName(workspaceName);

        return Mono.zip(Mono.just(dataset), workspaceMono)
                .map(tuple -> {
                    tuple.getT1().setWorkspaceName(workspaceName);
                    if (zoneSouthWestBottom.equals(zoneNorthEastTop)) {
                        List<GeorefBox> gridCells = GridAllocator.allocateDataset(zoneSouthWestBottom, tuple.getT1().getBbox(), tuple.getT2().getCellSize());
                        Map<String, List<GeorefBox>> datasetGrids = new HashMap<>();
                        datasetGrids.put(zoneSouthWestBottom, gridCells);
                        tuple.getT1().setRootDatablocks(datasetGrids);
                        saveGrids(gridCells, dataset, workspaceName).subscribe();

                    } else {
                        //Reproject northEast to the southWest zone
                        UTMCoord nw = GeoConverter.reprojectUTMCoordinate(tuple.getT1().getBbox().getNorthEastTop(), zoneSouthWestBottom);
                        GeorefBox georefBoxWithNECoordReprojected =
                                new GeorefBox(tuple.getT1().getBbox().getSouthWestBottom(), nw);

                        //Reproject southwest to the northEast zone
                        UTMCoord sw = GeoConverter.reprojectUTMCoordinate(tuple.getT1().getBbox().getSouthWestBottom(), zoneNorthEastTop);
                        GeorefBox georefBoxWithSWCoordReprojected =
                                new GeorefBox(sw, dataset.getBbox().getNorthEastTop());

                        //Obtain local grids for each of the new georefboxes defined
                        List<GeorefBox> NECoordReprojectedList = GridAllocator.allocateDataset(zoneSouthWestBottom, georefBoxWithNECoordReprojected, tuple.getT2().getCellSize());
                        List<GeorefBox> SWCoordReprojectedList = GridAllocator.allocateDataset(zoneNorthEastTop, georefBoxWithSWCoordReprojected, tuple.getT2().getCellSize());

                        //Update dataset and world grid
                        Map<String, List<GeorefBox>> grids = new HashMap<>();
                        grids.put(zoneSouthWestBottom, NECoordReprojectedList);
                        grids.put(zoneNorthEastTop, SWCoordReprojectedList);
                        tuple.getT1().setRootDatablocks(grids);
                        Flux<GridCell> gridsNECoord = saveGrids(NECoordReprojectedList, dataset, workspaceName);
                        Flux<GridCell> gridsSWCoord = saveGrids(SWCoordReprojectedList, dataset, workspaceName);
                        gridsNECoord.subscribe();
                        gridsSWCoord.subscribe();
                    }
                    return tuple.getT1();
                })
                .flatMap(datasetRepositoryInterface::save)
                .map(DatasetDTO::new)
                .log();
    }

    /**
     * Store grids that collide with a dataset bounding box
     *
     * @param grids
     * @param dataset
     * @param workspaceName
     * @return
     */
    private Flux<GridCell> saveGrids(List<GeorefBox> grids, Dataset dataset, String workspaceName) {

        return Flux.fromIterable(grids)
                .flatMap(grid -> gridCellRepositoryInterface.findById(workspaceName, grid)
                        .defaultIfEmpty(new GridCell(grid)))
                .doOnNext(grid -> logger.info(grid.toString()))
                .map(grid -> grid.addDataset(dataset.getDatasetName()))
                .flatMap(grid -> gridCellRepositoryInterface.save(workspaceName, grid));
    }


    /**
     * Indicates if the workspace exists or not
     *
     * @param workspaceName
     * @return
     */
    public Mono<Boolean> workspaceExists(String workspaceName) {
        return workspaceRepositoryInterface.existsByName(workspaceName);
    }

    /**
     * Retrieves a specific dataset, searching by workspace and dataset
     *
     * @param workspaceName name of the workspace
     * @param datasetName   name of the dataset
     * @return Mono with the dataset searched
     */
    public Mono<DatasetDTO> getDatasetByName(String workspaceName, String datasetName) {

        return datasetRepositoryInterface
                .findByWorkspaceAndDataset(workspaceName, datasetName) //Custom query that retrieve datasets satisfying both conditions
                .map(DatasetDTO::new)
                .log();
    }


    /**
     * @param workspaceName
     * @param datasetName
     * @return
     */
    public Mono<Boolean> datasetExists(String workspaceName, String datasetName) {
        return datasetRepositoryInterface.existsByWorkspaceAndDataset(workspaceName, datasetName)
                .log();
    }

    /**
     * Checks if the dataset we are going to put data in has already data associated to it.
     *
     * @param workspaceName
     * @param datasetName
     * @return False if the dataset does not have data associated, true otherwise. The boolean returned
     * is encapsulated in a Mono.
     */
    public Mono<Boolean> datasetHasDataAssociated(String workspaceName, String datasetName) {

        return datasetRepositoryInterface.findByWorkspaceAndDataset(workspaceName, datasetName)
                .map(Dataset::getFiles)
                .flatMap(path -> (path.isEmpty()) ? Mono.just(Boolean.FALSE) : Mono.just(Boolean.TRUE))
                .log();
    }

}
