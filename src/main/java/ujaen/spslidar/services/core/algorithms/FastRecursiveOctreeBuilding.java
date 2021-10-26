package ujaen.spslidar.services.core.algorithms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.services.tools.LasToolsService;
import ujaen.spslidar.services.tools.SystemFileStorageService;
import ujaen.spslidar.utils.properties.OctreeProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class FastRecursiveOctreeBuilding implements LasToolsAlgorithmInterface {

    Logger logger = LoggerFactory.getLogger(FastRecursiveOctreeBuilding.class);

    @Autowired
    LasToolsService lasToolsService;

    @Autowired
    OctreeProperties octreeProperties;

    @Autowired
    SystemFileStorageService systemFileStorageService;

    @Override
    public Flux<Datablock> octreeBuildingAlgorithm(Datablock datablock, int dataBlockSize) {

        Flux<Datablock> datablockFlux = Flux.empty();
        if (datablock.getDepth() == octreeProperties.getMaxDepth()) {
            logger.info("Max depth touched, no children incoming from " + datablock.getId());
            Mono<Datablock> datablockMono = lasToolsService.convertMaxDepthFileToBDReady(datablock);
            return datablockFlux.concatWith(datablockMono);

        } else {
            return lasToolsService.sampleDataFromFileWithKeepNth(datablock, dataBlockSize)
                    .flatMapMany(dblock -> {
                        if (dblock.getTmpOpsFile() != "") {
                            List<Datablock> children = dblock.createSubRegions();

                            return datablockFlux
                                    .concatWith(Flux.fromIterable(children)
                                            .flatMap(child -> lasToolsService.createChildNode(dblock.getTmpOpsFile(), child)
                                                    .filter(childCheck -> Files.exists(Path.of(childCheck.getTmpOpsFile())))
                                                    .doOnNext(childDatablock -> dblock.getChildren().add(childDatablock.getId()))
                                                    .flatMapMany(childRecursive ->
                                                            octreeBuildingAlgorithm(childRecursive, dataBlockSize)
                                                    )))
                                    .doOnComplete(() -> systemFileStorageService.deleteFiles(dblock.getTmpOpsFile()))
                                    .concatWith(Mono.just(dblock));
                        } else {
                            return datablockFlux.concatWith(Mono.just(dblock));
                        }
                    });
        }
    }


    @Override
    public Flux<Datablock> octreeBuildingAlgorithmScheduler(Datablock datablock, int dataBlockSize, Scheduler scheduler) {
        Flux<Datablock> datablockFlux = Flux.empty();
        if (datablock.getDepth() == octreeProperties.getMaxDepth()) {
            logger.info("Max depth touched, no children incoming from " + datablock.getId());
            Mono<Datablock> datablockMono = lasToolsService.convertMaxDepthFileToBDReady(datablock);
            return datablockFlux.concatWith(datablockMono);

        } else {
            return lasToolsService.sampleDataFromFileWithKeepNth(datablock, dataBlockSize)
                    .flatMapMany(dblock -> {
                        if (dblock.getTmpOpsFile() != "") {
                            List<Datablock> children = dblock.createSubRegions();

                            return datablockFlux
                                    .concatWith(Flux.fromIterable(children)
                                            .publishOn(scheduler)
                                            .flatMap(child -> lasToolsService.createChildNode(dblock.getTmpOpsFile(), child)
                                                    .filter(childDatablock -> Files.exists(Path.of(childDatablock.getTmpOpsFile())))
                                                    .doOnNext(childDatablock -> dblock.getChildren().add(childDatablock.getId()))
                                                    .flatMapMany(childDatablock ->
                                                            octreeBuildingAlgorithmScheduler(childDatablock, dataBlockSize, scheduler)
                                                    )))
                                    .doOnComplete(() -> systemFileStorageService.deleteFiles(dblock.getTmpOpsFile()))
                                    .concatWith(Mono.just(dblock));
                        } else {
                            return datablockFlux.concatWith(Mono.just(dblock));
                        }
                    });
        }
    }


    @Override
    public Flux<Datablock> octreeBuildingWithDistribution(Datablock datablock, List<Integer> sizes) {
        Integer dataBlockSize = sizes.get(datablock.getDepth());
        Flux<Datablock> datablockFlux = Flux.empty();
        if (datablock.getDepth() == octreeProperties.getMaxDepth()) {
            logger.info("Max depth touched, no children incoming from " + datablock.getId());
            Mono<Datablock> datablockMono = lasToolsService.convertMaxDepthFileToBDReady(datablock);
            return datablockFlux.concatWith(datablockMono);

        } else {
            return lasToolsService.sampleDataFromFileWithKeepNth(datablock, dataBlockSize)
                    .flatMapMany(dblock -> {
                        if (dblock.getTmpOpsFile() != "") {
                            List<Datablock> children = dblock.createSubRegions();

                            return datablockFlux
                                    .concatWith(Flux.fromIterable(children)
                                            .flatMap(child -> lasToolsService.createChildNode(dblock.getTmpOpsFile(), child)
                                                    .filter(childCheck -> Files.exists(Path.of(childCheck.getTmpOpsFile())))
                                                    .doOnNext(childDatablock -> dblock.getChildren().add(childDatablock.getId()))
                                                    .flatMapMany(childRecursive -> octreeBuildingWithDistribution(childRecursive, sizes)
                                                    )))
                                    .doOnComplete(() -> systemFileStorageService.deleteFiles(dblock.getTmpOpsFile()))
                                    .concatWith(Mono.just(dblock));

                        } else {
                            return datablockFlux.concatWith(Mono.just(dblock));
                        }
                    });
        }
    }

    @Override
    public Flux<Datablock> octreeBuildingWithDistributionScheduler(Datablock datablock, List<Integer> sizes, Scheduler scheduler) {
        Integer dataBlockSize = sizes.get(datablock.getDepth());
        Flux<Datablock> datablockFlux = Flux.empty();
        if (datablock.getDepth() == octreeProperties.getMaxDepth()) {
            logger.info("Max depth touched, no children incoming from " + datablock.getId());
            Mono<Datablock> datablockMono = lasToolsService.convertMaxDepthFileToBDReady(datablock);
            return datablockFlux.concatWith(datablockMono);

        } else {
            return lasToolsService.sampleDataFromFileWithKeepNth(datablock, dataBlockSize)
                    .flatMapMany(dblock -> {
                        if (dblock.getTmpOpsFile() != "") {
                            List<Datablock> children = dblock.createSubRegions();

                            return datablockFlux
                                    .concatWith(Flux.fromIterable(children)
                                            .publishOn(scheduler)
                                            .flatMap(child -> lasToolsService.createChildNode(dblock.getTmpOpsFile(), child)
                                                    .filter(childCheck -> Files.exists(Path.of(childCheck.getTmpOpsFile())))
                                                    .doOnNext(childDatablock -> dblock.getChildren().add(childDatablock.getId()))
                                                    .flatMapMany(childRecursive -> octreeBuildingWithDistributionScheduler(childRecursive, sizes, scheduler)
                                                    )))
                                    .doOnComplete(() -> systemFileStorageService.deleteFiles(dblock.getTmpOpsFile()))
                                    .concatWith(Mono.just(dblock));

                        } else {
                            return datablockFlux.concatWith(Mono.just(dblock));
                        }
                    });
        }
    }

}
