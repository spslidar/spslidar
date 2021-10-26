package ujaen.spslidar.repositories.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ujaen.spslidar.repositories.SchemaManagerInterface;

@Component
public class SchemaBuilderMongo implements SchemaManagerInterface {

    @Autowired
    ReactiveMongoOperations reactiveMongoOperations;

    @Override
    public Mono<Void> buildSchema() {
        return Mono.empty(); //Not needed in the Mongo implementation
    }

    @Override
    public Mono<Void> dropSchema() {
        return reactiveMongoOperations.getCollectionNames()
                .flatMap(reactiveMongoOperations::dropCollection)
                .then();
    }
}
