package ujaen.spslidar.repositories;


import reactor.core.publisher.Mono;

public interface SchemaManagerInterface {

    Mono<Void> buildSchema();

    Mono<Void> dropSchema();
}

