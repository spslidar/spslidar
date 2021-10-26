package ujaen.spslidar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "600000000")
public class DatablockTest {

    @Autowired
    DatablockEndpointsCaller datablockEndpointsCaller;

    private String workspaceName = "University of Jaen";
    private String datasetName = "Dataset University of Jaen";

    private final String errMessageNotFound = "Workspace or dataset not found";
    private final String errMessageHasData = "Dataset already has data associated";
    private final String messageAddedData = "Added dataset";


    @Test
    public void addDataset200() {
        datablockEndpointsCaller.getDatasetEndpointsCaller().addSimpleDataset();
        String datasetPath = "C:\\Users\\UJA\\Desktop\\Pruebas\\Test2";

        try {
            Mono<String> response = datablockEndpointsCaller.addDataset(workspaceName, datasetName,
                    datasetPath, HttpStatus.OK);
            StepVerifier.create(response)
                    .expectNext(messageAddedData)
                    .verifyComplete();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    @Test
    public void addDataset404() {
        datablockEndpointsCaller.getDatasetEndpointsCaller().addSimpleDataset();
        String datasetPath = "C:\\Users\\UJA\\Desktop\\Pruebas\\Test2";

        try {
            Mono<String> response = datablockEndpointsCaller.addDataset("Workspace that does not exist",
                    datasetName, datasetPath, HttpStatus.NOT_FOUND);
            StepVerifier.create(response)
                    .expectNext(errMessageNotFound)
                    .verifyComplete();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }


    @Test
    public void addDataset409() {
        datablockEndpointsCaller.getDatasetEndpointsCaller().addSimpleDataset();
        String datasetPath = "C:\\Users\\UJA\\Desktop\\Pruebas\\Test2";
        //First add the dataset correctly
        try {
            Mono<String> response = datablockEndpointsCaller.addDataset(workspaceName, datasetName, datasetPath, HttpStatus.OK);
            StepVerifier.create(response)
                    .expectNext(messageAddedData)
                    .verifyComplete();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        //Trigger a 409 inserting again the dataset
        try {
            Mono<String> response = datablockEndpointsCaller.addDataset(workspaceName, datasetName, datasetPath, HttpStatus.CONFLICT);
            StepVerifier.create(response)
                    .expectNext(errMessageHasData)
                    .verifyComplete();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    @Test
    public void getDatablockMetadata200() {
        addDataset200();

        String southWestQuery = "16N8200001150000";
        String northEastQuery = "16N8300001160000";

        Flux<String> response = datablockEndpointsCaller.getDatablock(workspaceName, datasetName, 0,
                Optional.of(southWestQuery), Optional.of(northEastQuery), HttpStatus.OK);

        assert response.count().block().equals(1L);
    }

    @Test
    public void getDatablockMetadata404() {
        addDataset200();

        String southWestQuery = "16N8200001150000";
        String northEastQuery = "16N8300001160000";
        Flux<String> response = datablockEndpointsCaller.getDatablock("Workspace that does not exist",
                datasetName, 0, Optional.of(southWestQuery), Optional.of(northEastQuery), HttpStatus.NOT_FOUND);

        StepVerifier.create(response)
                .expectNext(errMessageNotFound)
                .verifyComplete();

    }

    @Test
    public void getDatablockFile200() {
        addDataset200();

        String southWestQuery = "16N8200001150000";
        String northEastQuery = "16N8300001160000";

        datablockEndpointsCaller.getDatablockContent(workspaceName, datasetName, 0,
                southWestQuery, northEastQuery, HttpStatus.OK, Boolean.TRUE);
    }

    @Test
    public void getDatablockFile404() {
        addDataset200();

        String southWestQuery = "16N8200001150000";
        String northEastQuery = "16N8300001160000";

        datablockEndpointsCaller.getDatablockContent(workspaceName, datasetName, 0,
                southWestQuery, northEastQuery, HttpStatus.NOT_FOUND, Boolean.FALSE);
    }

    @Test
    public void getCompleteDataset(){
        addDataset200();

        datablockEndpointsCaller.getCompleteDataset(workspaceName, datasetName, HttpStatus.OK, Boolean.TRUE);

    }


}
