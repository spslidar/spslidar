/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatablockDTO;
import ujaen.spslidar.Exceptions.DatasetHasDataAssociated;
import ujaen.spslidar.Exceptions.ElementNotFound;
import ujaen.spslidar.Exceptions.NoUTMZoneInFile;
import ujaen.spslidar.services.core.DatablockService;
import ujaen.spslidar.services.core.DatablockServiceCommonUtils;
import ujaen.spslidar.services.core.DatasetService;

/**
 * Controller class to manage the requests associated to the Datablock entity
 */
@RestController
@RequestMapping("/spslidar/workspaces/")
public class DatablockController {

    private final DatablockService datablockService;
    private final DatablockServiceCommonUtils datablockServiceCommonUtils;
    private final DatasetService datasetService;

    private final String errMessageNotFound = "Workspace or dataset not found";
    private final String errMessageHasData = "Dataset already has data associated";
    private final String messageAddedData = "Added dataset";


    Logger logger = LoggerFactory.getLogger(DatablockController.class);

    public DatablockController(DatablockService datablockService,
                               DatablockServiceCommonUtils datablockServiceCommonUtils,
                               DatasetService datasetService) {
        this.datablockService = datablockService;
        this.datablockServiceCommonUtils = datablockServiceCommonUtils;
        this.datasetService = datasetService;
    }


    /**
     * Search a datablock associated to a workspace and dataset
     *
     * @param workspace_name name of the workspace
     * @param dataset_name   name of the dataset
     * @param datablock_id   id of the datablock
     * @param sw_coord       south west coordinate of the geoquery
     * @param ne_coord       north east coordinate of the geoquery
     * @return If operation is successful, it will return one or more datablocks depending on whether the coordinates
     * of a grid were specified (will return at max 1) or not (will return at max as many datablocks as octrees were generated
     * for the dataset). If no workspace, dataset or datablock was found, an error handler will manage the request.
     */
    @GetMapping(value = "{workspace_name}/datasets/{dataset_name}/datablocks/{datablock_id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<DatablockDTO> getDatablock(@PathVariable String workspace_name,
                                           @PathVariable String dataset_name,
                                           @PathVariable String datablock_id,
                                           @RequestParam(defaultValue = "", required = false) String sw_coord,
                                           @RequestParam(defaultValue = "", required = false) String ne_coord) {


        return datablockService
                .getDatablockData(workspace_name, dataset_name, Integer.parseInt(datablock_id), sw_coord, ne_coord)
                .switchIfEmpty(Mono.error(new ElementNotFound()));

    }


    /**
     * Retrieve the data associated to a datablock
     *
     * @param workspace_name name of the workspace
     * @param dataset_name   name of the dataset
     * @param datablock_id   id of the datablock
     * @param sw_coord       south west coordinate of the geoquery
     * @param ne_coord       north east coordinate of the geoquery
     * @return If the operation is successful, will return the data of the point associated to the datablock.
     * If no workspace, dataset or datablock was found, an error handler will manage the request.
     */
    @GetMapping(value = "{workspace_name}/datasets/{dataset_name}/datablocks/{datablock_id}/data")
    public Flux<DataBuffer> getDatablockData(@PathVariable String workspace_name,
                                             @PathVariable String dataset_name,
                                             @PathVariable int datablock_id,
                                             @RequestParam(defaultValue = "") String sw_coord,
                                             @RequestParam(defaultValue = "") String ne_coord) {


        return datablockServiceCommonUtils.dataBlockExists(workspace_name, dataset_name, datablock_id, sw_coord, ne_coord)
                .flatMapMany(aBoolean -> {
                    if (!aBoolean)
                        throw new ElementNotFound();
                    else
                        return datablockService
                                .getDatablockFile(workspace_name, dataset_name, datablock_id, sw_coord, ne_coord)
                                .doOnComplete(() -> {
                                    logger.info("Served file: " + workspace_name + "_" + dataset_name + "_" + datablock_id);
                                });
                });

    }


    /**
     * Insert a point cloud to a dataset
     *
     * @param workspace_name name of the workspace
     * @param dataset_name   name of the dataset
     * @param files          files that compose the data of the point cloud
     * @return Return code and informative message with the result of the operation
     */
    @PutMapping(value = "{workspace_name}/datasets/{dataset_name}/data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity> addPointCloudToDataset(@PathVariable String workspace_name,
                                                       @PathVariable String dataset_name,
                                                       @RequestPart("files") Flux<FilePart> files) {

        logger.info("Adding point cloud");
        Mono<Boolean> datasetExists = datasetService.datasetExists(workspace_name, dataset_name);
        Mono<Boolean> hasData = datasetService.datasetHasDataAssociated(workspace_name, dataset_name);


        return datasetExists.flatMap(datasetExistsboolean -> {
            if (datasetExistsboolean) {
                return hasData.flatMap(booleanHasData -> {
                    if (booleanHasData) {
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errMessageHasData));
                    } else {
                        return datablockService.addDataToDataset(workspace_name, dataset_name, files)
                                .map(aBoolean -> ResponseEntity.status(HttpStatus.OK).body(messageAddedData));
                    }
                });
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errMessageNotFound));
            }
        });

    }

    /**
     * Retrieve the complete point cloud associated to a dataset
     *
     * @param workspace_name name of the workspace
     * @param dataset_name   name of the dataset
     * @return File with the complete point cloud
     */
    @ResponseStatus(code = HttpStatus.OK)
    @GetMapping(value = "{workspace_name}/datasets/{dataset_name}/data")
    public Flux<Resource> getCompleteDataset(@PathVariable String workspace_name,
                                             @PathVariable String dataset_name) {

        Mono<Boolean> datasetExists = datasetService.datasetExists(workspace_name, dataset_name);

        return datasetExists.flatMapMany(aBoolean ->
                aBoolean ? datablockService.getCompleteDataset(workspace_name, dataset_name) : Mono.error(new ElementNotFound()));
    }



    /**
     * Handler called in case either the workspace or the dataset are not in the system
     * @return 404 code with informative message
     */
    @ExceptionHandler(ElementNotFound.class)
    public Mono<ResponseEntity> elementNotFound() {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errMessageNotFound));
    }

    /**
     * Handler called in case the dataset already has a point cloud associated
     * @return 409 with informative message
     */
    @ExceptionHandler(DatasetHasDataAssociated.class)
    public Mono<ResponseEntity> datasetHasData() {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errMessageHasData));
    }


    @ExceptionHandler(NoUTMZoneInFile.class)
    public Mono<ResponseEntity> noUTMZoneInFile(){
        return Mono.just(ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("A UTM Zone file could not be found in the sent files. You" +
                        "can add one by using las2las from LASTools like las2las -i oldFile.laz -o newFile.laz -utm 30N"));
    }


}
