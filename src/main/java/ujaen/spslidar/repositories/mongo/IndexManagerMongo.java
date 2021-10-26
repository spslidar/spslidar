package ujaen.spslidar.repositories.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ujaen.spslidar.repositories.CollectionsManager;

@Component
public class IndexManagerMongo {

    private static final String collectionExtension = "_datablocks";

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public Mono<String> createIndex(String workspace){
        String collection = CollectionsManager.cleanCollectionName(workspace)+collectionExtension;
        return this.reactiveMongoTemplate.indexOps(collection)
                .ensureIndex(new Index().on("datasetName", Sort.Direction.ASC)
                        .on("node", Sort.Direction.ASC));

    }

}
