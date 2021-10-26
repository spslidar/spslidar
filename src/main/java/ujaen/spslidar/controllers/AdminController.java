package ujaen.spslidar.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ujaen.spslidar.repositories.PerformanceStatsServiceInterface;
import ujaen.spslidar.repositories.SchemaManagerInterface;
import ujaen.spslidar.utils.properties.OctreeProperties;

@RestController
@RequestMapping("/spslidar/")
public class AdminController {

    @Autowired
    ReactiveMongoOperations operations;

    @Autowired
    PerformanceStatsServiceInterface performanceStatsServiceInterface;

    @Autowired
    OctreeProperties octreeProperties;

    @Autowired
    SchemaManagerInterface schemaManager;

    @DeleteMapping(value = "database")
    public Mono<ResponseEntity> resetDatabase() {

        return schemaManager.dropSchema()
                .then(schemaManager.buildSchema())
                .thenReturn(ResponseEntity.ok("Reseting database"));
    }

    @GetMapping(value = "database")
    public Mono<ResponseEntity> getDatabaseSize() {

        Mono<String> databaseSize = performanceStatsServiceInterface.databaseSize();
        return databaseSize.map(dbSize -> ResponseEntity.status(HttpStatus.OK).body(dbSize));
    }



    @GetMapping("/workspaces/{workspace_name}/datasets/{dataset_name}/size")
    public Mono<ResponseEntity> getOctreeSize(@PathVariable String workspace_name,
                                              @PathVariable String dataset_name) {

        Mono<Long> longMono = performanceStatsServiceInterface.getOctreeSize(workspace_name, dataset_name);
        return longMono.map(aLong -> ResponseEntity.status(HttpStatus.OK).body(aLong));

    }

    @GetMapping("/workspaces/{workspace_name}/datasets/{dataset_name}/depth")
    public Mono<ResponseEntity> getMaxDepth(@PathVariable String workspace_name,
                                            @PathVariable String dataset_name) {

        Mono<Integer> integerMono = performanceStatsServiceInterface.getMaxDepth(workspace_name, dataset_name);
        return integerMono.map(integer -> ResponseEntity.status(HttpStatus.OK).body(integer));

    }


    @PutMapping("octree/{size}")
    public Mono<Integer> updateMaxOctreeSize(@PathVariable Integer size) {
        System.out.println("Previous max octree size value was: " + size);
        octreeProperties.setMaxDepth(size);
        System.out.println("New max octree size value is: " + size);
        return Mono.just(octreeProperties.getMaxDepth());
    }


}
