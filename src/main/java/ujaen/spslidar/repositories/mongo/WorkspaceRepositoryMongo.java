package ujaen.spslidar.repositories.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.database.mongo.WorkspaceDBDTO;
import ujaen.spslidar.entities.Workspace;
import ujaen.spslidar.repositories.WorkspaceRepositoryInterface;

@Repository
public class WorkspaceRepositoryMongo implements WorkspaceRepositoryInterface {

    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<Workspace> findAll() {
        return reactiveMongoTemplate.findAll(WorkspaceDBDTO.class).map(WorkspaceDBDTO::workspaceFromDTO);
    }

    @Override
    public Mono<Workspace> findByName(String workspaceName) {
        return reactiveMongoTemplate.findById(workspaceName, WorkspaceDBDTO.class).map(WorkspaceDBDTO::workspaceFromDTO);
    }

    @Override
    public Mono<Boolean> existsByName(String workspaceName) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(workspaceName));
        return reactiveMongoTemplate.exists(query, WorkspaceDBDTO.class);

    }

    @Override
    public Mono<Workspace> save(Workspace workspace){
        WorkspaceDBDTO workspaceDBDTO = new WorkspaceDBDTO(workspace);
        return reactiveMongoTemplate
                .save(workspaceDBDTO)
                .map(WorkspaceDBDTO::workspaceFromDTO);

    }
}
