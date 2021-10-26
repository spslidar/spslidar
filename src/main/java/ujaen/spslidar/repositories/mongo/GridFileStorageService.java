package ujaen.spslidar.repositories.mongo;


import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsUpload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.repositories.FileRepositoryInterface;
import ujaen.spslidar.services.tools.LazReaderInterface;

@Service
public class GridFileStorageService implements FileRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(GridFileStorageService.class);

    @Value("${persistence.chunkSize}")
    int chunkSize;

    ResourceLoader resourceLoader;

    ReactiveGridFsTemplate reactiveGridFsTemplate;

    LazReaderInterface lazReaderInterface;

    @Autowired
    public GridFileStorageService(ResourceLoader resourceLoader, ReactiveGridFsTemplate reactiveGridFsTemplate, @Qualifier("lazReaderServicePylasImplementation") LazReaderInterface lazReaderInterface) {
        this.resourceLoader = resourceLoader;
        this.reactiveGridFsTemplate = reactiveGridFsTemplate;
        this.lazReaderInterface = lazReaderInterface;
    }

    /**
     * Returns the GridFsResource associated to the workspace, dataset and id indicated
     *
     * @param objectId
     * @return
     */
    @Override
    public Flux<DataBuffer> getFile(ObjectId objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(objectId));

        return reactiveGridFsTemplate.findFirst(query)
                .flatMap(reactiveGridFsTemplate::getResource)
                .flatMapMany(ReactiveGridFsResource::getContent)
                .name("db.file.get")
                .metrics();
    }


    /**
     * Adds a file to GridFS. This file will be identified by the datablock and dataset passed
     *
     * @param datablock contains the node ID and path to the file we want to insert
     * @param dataset     dataset to which the datablock is associated, in order to retrieve the
     *                  workspace, dataset and date of acquisition so we can add it to the file metadata
     * @return
     */
    @Override
    public Mono<AbstractDatablock> addFile(AbstractDatablock datablock, Dataset dataset) {

        Resource resource = resourceLoader.getResource("file:" + datablock.getLazFileAssociated());

        Publisher<DataBuffer> file = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 1024 * 1024);

        ReactiveGridFsUpload reactiveGridFsUpload =
                ReactiveGridFsUpload.fromPublisher(file)
                        .id(new ObjectId())
                        .filename(datablock.getLazFileAssociated())
                        .chunkSize(chunkSize)
                        .build();

        return reactiveGridFsTemplate.store(reactiveGridFsUpload)
                .map(o -> {
                    datablock.setObjectId((ObjectId) o);
                    return datablock;
                });
    }

    @Override
    public Flux<DataBuffer> getFile(String workspaceName, String datasetName, int node, GeorefBox box) {
        return Flux.error(new RuntimeException("Method not available for Mongo implementation"));

    }
}
