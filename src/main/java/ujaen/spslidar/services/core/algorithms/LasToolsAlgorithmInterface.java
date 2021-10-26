package ujaen.spslidar.services.core.algorithms;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import ujaen.spslidar.entities.Datablock;

import java.util.List;

public interface LasToolsAlgorithmInterface {


    public Flux<Datablock> octreeBuildingAlgorithm(Datablock datablock, int dataBlockSize);

    public Flux<Datablock> octreeBuildingAlgorithmScheduler(Datablock datablock, int dataBlockSize, Scheduler scheduler);

    public Flux<Datablock> octreeBuildingWithDistribution(Datablock datablock, List<Integer> sizes);

    public Flux<Datablock> octreeBuildingWithDistributionScheduler(Datablock datablock, List<Integer> sizes, Scheduler scheduler);
}
