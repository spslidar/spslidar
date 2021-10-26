package ujaen.spslidar.repositories.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;

@Configuration
public class ReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.database}")
    String database;

    private MongoClient mongoClient;
    private ReactiveMongoOperations operations;

    public ReactiveMongoConfiguration(MongoClient mongoClient, ReactiveMongoOperations operations) {
        this.mongoClient = mongoClient;
        this.operations = operations;
    }


    @Bean
    @Order(1)
    @ConditionalOnExpression("${database.reset:true}")
    CommandLineRunner resetDatabase() {
        return args -> {
            operations.getCollectionNames()
                    .doOnNext(s -> {
                        System.out.println("Dropping " + s);
                        operations.dropCollection(s).subscribe();
                    }).subscribe();

        };
    }

    @Bean
    @Order(2)
    CommandLineRunner initiateGridFS() {
        return args -> {
            boolean fsChunksExists = operations.getCollectionNames().toStream()
                    .anyMatch(collection -> collection.equals("fs.chunks"));

            boolean fsFilesExits = operations.getCollectionNames().toStream()
                    .anyMatch(collection -> collection.equals("fs.chunks"));

            if (!fsChunksExists)
                operations.createCollection("fs.chunks").subscribe();

            if (!fsFilesExits)
                operations.createCollection("fs.files").subscribe();

        };
    }


}