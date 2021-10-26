package ujaen.spslidar.services.core.algorithms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.services.tools.LasToolsService;
import ujaen.spslidar.utils.properties.OctreeProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SlowRecursiveOctreeBuilding implements LasToolsAlgorithmInterface {

    Logger logger = LoggerFactory.getLogger(FastRecursiveOctreeBuilding.class);

    @Autowired
    LasToolsService lasToolsService;

    @Autowired
    OctreeProperties octreeProperties;

    @Override
    public Flux<Datablock> octreeBuildingAlgorithm(Datablock datablock, int dataBlockSize) {
        Flux<Datablock> datablockFlux = Flux.empty();

        return lasToolsService
                .changeUserData(datablock, 0)
                .flatMap(dblock -> {
                    String parentFile = dblock.getTmpOpsFile();
                    return lasToolsService
                            .sampleDataFromFile(dblock, dataBlockSize)
                            .flatMap(sampledDblock -> lasToolsService.changeUserData(sampledDblock, 1))
                            .flatMap(childUserDataChangedDblock ->
                                    lasToolsService.mergeWithDuplicates(childUserDataChangedDblock, parentFile));
                })
                .flatMap(dblock -> lasToolsService.extractDuplicates(dblock, 0))
                .flatMapMany(dblock -> {
                    String parentFile = dblock.getTmpOpsFile();
                    if (dblock.getNumberOfPoints() >= dataBlockSize) {
                        List<Datablock> children = dblock.createSubRegions();
                        //Set children list
                        dblock.setChildren(children.stream()
                                .map(child -> child.getId())
                                .collect(Collectors.toList()));

                        return datablockFlux
                                .concatWith(Mono.just(dblock))
                                .concatWith(Flux.fromIterable(children)
                                        .parallel()
                                        .runOn(Schedulers.boundedElastic())
                                        .flatMap(child -> lasToolsService.createChildNode(parentFile, child)
                                                .filter(childCheck -> Files.exists(Path.of(childCheck.getTmpOpsFile())))
                                                .flatMapMany(childRecursive -> {
                                                    return octreeBuildingAlgorithm(childRecursive, dataBlockSize);
                                                })));

                    } else {
                        return datablockFlux.concatWith(Mono.just(dblock));
                    }

                });
    }

    @Override
    public Flux<Datablock> octreeBuildingAlgorithmScheduler(Datablock datablock, int dataBlockSize, Scheduler scheduler) {
        return null;
    }

    @Override
    public Flux<Datablock> octreeBuildingWithDistribution(Datablock datablock, List<Integer> sizes) {
        return null;
    }

    @Override
    public Flux<Datablock> octreeBuildingWithDistributionScheduler(Datablock datablock, List<Integer> sizes, Scheduler scheduler) {
        return null;
    }
}
