package ujaen.spslidar.repositories.mongo;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.database.mongo.DatasetDBDTO;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.repositories.CollectionsManager;
import ujaen.spslidar.repositories.DatasetRepositoryInterface;

import java.time.LocalDateTime;

@Repository
public class DatasetRepositoryMongo implements DatasetRepositoryInterface {

    private static final String collectionExtension = "_datasets";
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public DatasetRepositoryMongo(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    public Mono<Boolean> existsByWorkspaceAndDataset(String workspaceName, String datasetName) {
        String collection = CollectionsManager.cleanCollectionName(workspaceName) + collectionExtension;
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(datasetName));

        return reactiveMongoTemplate.exists(query, collection);
    }


    @Override
    public Flux<Dataset> findByWorskpaceNameAndUTMZoneAndTimeWindow(String workspaceName, String UTMZone, LocalDateTime fromDate, LocalDateTime toDate) {
        Query query = new Query();
        String collection = CollectionsManager.cleanCollectionName(workspaceName) + collectionExtension;

        if (!UTMZone.isEmpty()) {
            query.addCriteria(Criteria.where("bbox.northEastTop.zone").is(UTMZone));
        }

        query.addCriteria(Criteria.where("date").gte(fromDate).lte(toDate));

        return reactiveMongoTemplate.find(query, DatasetDBDTO.class, collection)
                .map(DatasetDBDTO::datasetFromDTO)
                .map(dataset -> setWorkspace(dataset, workspaceName));
    }

    @Override
    public Mono<Dataset> findByWorkspaceAndDataset(String workspaceName, String datasetName) {
        String collection = CollectionsManager.cleanCollectionName(workspaceName) + collectionExtension;
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(datasetName));
        return reactiveMongoTemplate.findOne(query, DatasetDBDTO.class, collection)
                .map(DatasetDBDTO::datasetFromDTO)
                .map(dataset -> setWorkspace(dataset, workspaceName))
                .doOnNext(System.out::println);
    }

    @Override
    public Mono<Dataset> save(Dataset dataset) {
        String collection = CollectionsManager.cleanCollectionName(dataset.getWorkspaceName()) + collectionExtension;
        DatasetDBDTO datasetDBDTO = new DatasetDBDTO(dataset);

        return reactiveMongoTemplate.save(datasetDBDTO, collection)
                .map(DatasetDBDTO::datasetFromDTO)
                .map(m -> setWorkspace(m, dataset.getWorkspaceName()));
    }

    @Override
    public Mono<Dataset> update(Dataset dataset) {
        return this.save(dataset);
    }

    @Override
    public Mono<Dataset> findByWorkspaceAndDatasetAndTimeWindow(String workspaceName, String datasetName, LocalDateTime fromDate, LocalDateTime toDate) {
        String collection = CollectionsManager.cleanCollectionName(workspaceName) + collectionExtension;

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(datasetName));
        query.addCriteria(Criteria.where("date").gte(fromDate).lte(toDate));

        return reactiveMongoTemplate.findOne(query, DatasetDBDTO.class, collection)
                .map(DatasetDBDTO::datasetFromDTO)
                .map(m -> setWorkspace(m, workspaceName));
    }

    /**
     * Adds the workspaceName to the dataset as that attribute is not stored in Mongo to avoid redundancy
     * with the collection name
     *
     * @param dataset
     * @param workspaceName
     * @return
     */
    private Dataset setWorkspace(Dataset dataset, String workspaceName) {
        dataset.setWorkspaceName(workspaceName);
        return dataset;
    }

}
