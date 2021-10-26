package ujaen.spslidar.repositories;

import reactor.core.publisher.Mono;

public interface PerformanceStatsServiceInterface {

    /**
     * Obtain the total number of elements that conform the structure generated
     *
     * @param workspace
     * @param dataset
     * @return
     */
    Mono<Long> getOctreeSize(String workspace, String dataset);

    /**
     * Obtain the maximum depth associated to a node of the structure generated
     *
     * @param workspace
     * @param dataset
     * @return
     */
    Mono<Integer> getMaxDepth(String workspace, String dataset);



    Mono<String> databaseSize();


}
