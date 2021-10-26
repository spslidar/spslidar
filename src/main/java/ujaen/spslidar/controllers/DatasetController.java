/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatasetDTO;
import ujaen.spslidar.Exceptions.DatasetAlreadyExists;
import ujaen.spslidar.Exceptions.ElementNotFound;
import ujaen.spslidar.Exceptions.WorkspaceNotFoundException;
import ujaen.spslidar.services.core.DatasetService;
import ujaen.spslidar.services.core.WorkspaceService;

import java.time.LocalDateTime;

/**
 * Controller class to manage the requests associated to the Dataset entity
 */
@RestController
@RequestMapping("/spslidar/workspaces/")
public class DatasetController {

    private final DatasetService datasetService;
    private final WorkspaceService workspaceService;

    public DatasetController(DatasetService datasetService,
                             WorkspaceService workspaceService) {
        this.datasetService = datasetService;
        this.workspaceService = workspaceService;
    }

    /**
     * Search the datasets of a workspace that fit a temporal and geographic query
     *
     * @param workspace_name name of the workspace
     * @param sw_coord       south west coordinate of the geoquery
     * @param ne_coord       north east coordinate of the geoquery
     * @param from_date      lower limit of the temporal query
     * @param to_date        upper limit of the temporal query
     * @return If the operation was successful it will return the dataset metadata associated to the
     * parameters specified. Otherwise, an error hanlder will manage the result.
     */
    @GetMapping(value = "{workspace_name}/datasets", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<DatasetDTO> getWorkspaceDatasets(
            @PathVariable String workspace_name,
            @RequestParam(defaultValue = "", required = false) String sw_coord,
            @RequestParam(defaultValue = "", required = false) String ne_coord,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).of(0001,1,1,0,0)}", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from_date,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).of(9999,12,31,23,59)}", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to_date) {

        Mono<Boolean> booleanMonoWorkspaceExists = datasetService.workspaceExists(workspace_name);

        return booleanMonoWorkspaceExists
                .flatMapMany(aBoolean -> {
                    if (!aBoolean) {
                        throw new WorkspaceNotFoundException();
                    } else {
                        return datasetService.getDatasets(workspace_name, sw_coord, ne_coord, from_date, to_date);
                    }
                })
                .log();


    }

    /**
     * Search for a specific dataset by the workspace it belongs to and its name
     *
     * @param workspace_name name of the workspace
     * @param dataset_name   name of the dataset
     * @return If the operation is successful, it will return the metadata associated to the corresponding dataset.
     */
    @GetMapping(value = "/{workspace_name}/datasets/{dataset_name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity> getDatasetByName(
            @PathVariable String workspace_name,
            @PathVariable String dataset_name) {

        return datasetService.getDatasetByName(workspace_name, dataset_name)
                .map(dataset -> ResponseEntity.status(HttpStatus.OK).body(dataset))
                .switchIfEmpty(Mono.error(new ElementNotFound()))
                .cast(ResponseEntity.class);

    }

    /**
     * Inserts a new dataset in the system
     *
     * @param workspace_name workspace the dataset will be associated to
     * @param dataset        metadata of the dataset to be inserted
     * @return If the operation was successful it will return the metadata associated to the dataset
     * created, otherwise an error handler will manage the result of the request
     */
    @PostMapping(value = "{workspace_name}/datasets", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity> addDataset(
            @PathVariable String workspace_name,
            @RequestBody DatasetDTO dataset) {

        Mono<Boolean> workspaceMonoExists = workspaceService.workspaceExists(workspace_name);
        Mono<Boolean> datasetMonoExists = datasetService.datasetExists(workspace_name, dataset.getName());

        return workspaceMonoExists.zipWith(datasetMonoExists)
                .doOnNext(checks -> {
                    if (!checks.getT1())
                        throw new ElementNotFound();
                    if (checks.getT2())
                        throw new DatasetAlreadyExists();
                })
                .flatMap(objects -> datasetService.addDataset(workspace_name, dataset))
                .map(datasetDTO -> ResponseEntity.status(HttpStatus.CREATED).body(datasetDTO))
                .cast(ResponseEntity.class);

    }


    /**
     * Handler called in case the workspace specified by the user isn't in the system
     *
     * @return 404 code with informative message
     */
    @ExceptionHandler(WorkspaceNotFoundException.class)
    public Mono<ResponseEntity> workspaceNotFoundHandlerException() {

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown workspace"));
    }


    /**
     * Handler called in case either the workspace or the dataset are not in the system
     *
     * @return 404 code with informative message
     */
    @ExceptionHandler(ElementNotFound.class)
    public Mono<ResponseEntity> elementNotFoundHandlerException() {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown workspace or dataset"));
    }


    /**
     * Handler called in case the dataset to be created has a name that is already registered
     * for that workspace
     *
     * @return 409 code with informative message
     */
    @ExceptionHandler(DatasetAlreadyExists.class)
    public Mono<ResponseEntity> datasetAlreadyExists() {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body("Dataset already exists in workspace"));
    }


}
