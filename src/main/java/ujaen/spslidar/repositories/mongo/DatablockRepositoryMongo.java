package ujaen.spslidar.repositories.mongo;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.database.mongo.DatablockDBDTO;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.Datablock;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.repositories.CollectionsManager;
import ujaen.spslidar.repositories.DatablockRepositoryInterface;

/**
 * Datablock repository implementation for Mongo
 */
@Repository
public class DatablockRepositoryMongo implements DatablockRepositoryInterface {

    private static final String collectionExtension = "_datablocks";
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public DatablockRepositoryMongo(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }


    @Override
    public Flux<AbstractDatablock> findDatablockByWorkspaceAndDatasetAndNode(String workspace, String dataset, int node) {

        String collection = getCollectionName(workspace);
        Query query = new Query();
        query.addCriteria(Criteria.where("datasetName").is(dataset));
        query.addCriteria(Criteria.where("node").is(node));
        return reactiveMongoTemplate
                .find(query, DatablockDBDTO.class, collection).map(DatablockDBDTO::fromDatablockDBDTO);
    }


    @Override
    public Mono<Boolean> existsByWorkspaceAndDatasetAndNodeAndGridCell(
            String workspace, String dataset, int node, GeorefBox grid) {

        String collection = getCollectionName(workspace);

        Query query = new Query();
        query.addCriteria(Criteria.where("datasetName").is(dataset));
        query.addCriteria(Criteria.where("node").is(node));
        query.addCriteria(Criteria.where("cell").is(grid));
        return reactiveMongoTemplate.exists(query, DatablockDBDTO.class, collection);


    }

    @Override
    public Mono<AbstractDatablock> findDatablockByWorkspaceAndDatasetAndNodeAndGridCell(String workspace, String dataset, int node, GeorefBox grid) {
        String collection = getCollectionName(workspace);

        Query query = new Query();
        query.addCriteria(Criteria.where("datasetName").is(dataset));
        query.addCriteria(Criteria.where("node").is(node));
        query.addCriteria(Criteria.where("cell").is(grid));
        return reactiveMongoTemplate.find(query, DatablockDBDTO.class, collection).next()
                .map(DatablockDBDTO::fromDatablockDBDTO)
                .cast(AbstractDatablock.class)
                .name("db.datablock.get")
                .metrics();

    }

    @Override
    public Mono<AbstractDatablock> save(AbstractDatablock abstractDatablock, String workspace, String datasetName) {
        String collection = getCollectionName(workspace);

        if (((Datablock) abstractDatablock).getObjectId().toString().isEmpty()) {
            return Mono.error(new RuntimeException("No GridFS file ID found"));
        }

        DatablockDBDTO datablockDBDTO = new DatablockDBDTO(abstractDatablock, datasetName, ((Datablock) abstractDatablock).getObjectId());
        return reactiveMongoTemplate.save(datablockDBDTO, collection)
                .map(DatablockDBDTO::fromDatablockDBDTO);
    }


    @Override
    public Flux<AbstractDatablock> findAllDatablocksInDataset(String workspace, String dataset) {

        String collection = CollectionsManager.cleanCollectionName(workspace) + collectionExtension;
        Query query = new Query();
        query.addCriteria(Criteria.where("datasetName").is(dataset));

        return reactiveMongoTemplate.find(query, DatablockDBDTO.class, collection)
                .map(DatablockDBDTO::fromDatablockDBDTO);

    }



    private String getCollectionName(String workspaceName) {
        return CollectionsManager.cleanCollectionName(workspaceName) + collectionExtension;
    }

}
