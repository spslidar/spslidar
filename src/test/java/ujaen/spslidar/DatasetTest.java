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
import ujaen.spslidar.DTOs.http.DatasetDTO;
import ujaen.spslidar.DTOs.http.WorkspaceDTO;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.UTMCoord;

import java.time.LocalDateTime;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "600000000")
public class DatasetTest {

    @Autowired
    DatasetEndpointsCaller datasetEndpointsCaller;

    private String workspaceName = "University of Jaen";

    @Test
    public void getDatasetsInWorkspace() {
        datasetEndpointsCaller.addSimpleDataset();

        Flux<String> datasetDTOFlux = datasetEndpointsCaller
                .getDatasetsInWorkspace(workspaceName, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), HttpStatus.OK);

        StepVerifier.create(datasetDTOFlux.count())
                .expectNextCount(1L)
                .verifyComplete();
    }

    @Test
    public void getDatasetsInWorkspaceAndInGeoWindow() {
        datasetEndpointsCaller.addSimpleDataset();
        String sw = "16N8250001158000";
        String ne = "16N8300001160000";

        Flux<String> datasetDTOFlux = datasetEndpointsCaller
                .getDatasetsInWorkspace(workspaceName, Optional.of(sw), Optional.of(ne),
                        Optional.empty(), Optional.empty(), HttpStatus.OK);

        StepVerifier.create(datasetDTOFlux.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void getDatasetsInWorkspaceAndInGeoAndTimeWindows() {
        datasetEndpointsCaller.addSimpleDataset();

        String sw = "16N8250001158000";
        String ne = "16N8300001160000";
        LocalDateTime fromDate = LocalDateTime.of(2015, 1, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2022, 3, 1, 0, 0);

        Flux<String> datasetDTOFlux = datasetEndpointsCaller
                .getDatasetsInWorkspace(workspaceName, Optional.of(sw), Optional.of(ne),
                        Optional.of(fromDate), Optional.of(toDate), HttpStatus.OK);

        StepVerifier.create(datasetDTOFlux.count())
                .expectNext(1L)
                .verifyComplete();

    }

    @Test
    public void getDatasetByName200() {
        datasetEndpointsCaller.addSimpleDataset();

        String datasetName = "Dataset University of Jaen";

        Mono<DatasetDTO> datasetDTO = datasetEndpointsCaller
                .getDatasetByName(workspaceName, datasetName, HttpStatus.OK)
                .map(datasetEndpointsCaller::mapToDatasetDTO);


        StepVerifier.create(datasetDTO.map(DatasetDTO::getName))
                .expectNext(datasetName)
                .verifyComplete();

    }

    @Test
    public void getDatasetByName404() {
        datasetEndpointsCaller.addSimpleDataset();

        String datasetName = "Dataset University of Jaen Error";
        String errMessage = "Unknown workspace or dataset";

        Mono<String> response = datasetEndpointsCaller
                .getDatasetByName(workspaceName, datasetName, HttpStatus.NOT_FOUND);

        StepVerifier.create(response)
                .expectNext(errMessage)
                .verifyComplete();

    }

    @Test
    public void addDataset201() {
        datasetEndpointsCaller.addSimpleDataset();

    }

    @Test
    public void addDataset404() {
        datasetEndpointsCaller.getWorkspaceEndpointsCaller().addSimpleWorkspace();
        String errMessage = "Unknown workspace or dataset";

        GeorefBox georefBox1 = new GeorefBox(
                UTMCoord.builder()
                        .easting(826083)
                        .northing(1158000)
                        .zone("16N")
                        .build(),
                UTMCoord.builder()
                        .easting(829575)
                        .northing(1159506)
                        .zone("16N")
                        .build());

        DatasetDTO m1 = new DatasetDTO("Dataset University of Jaen",
                "Descripción dataset 1", LocalDateTime.now(), georefBox1,
                100000, "LAZ");

        Mono<String> response = datasetEndpointsCaller.addDataset("Workspace that does not exist", m1, HttpStatus.NOT_FOUND);
        StepVerifier.create(response)
                .expectNext(errMessage)
                .verifyComplete();

    }

    @Test
    public void addDataset409() {
        datasetEndpointsCaller.addSimpleDataset();
        String errMessage = "Dataset already exists in workspace";

        GeorefBox georefBox1 = new GeorefBox(
                UTMCoord.builder()
                        .easting(826083)
                        .northing(1158000)
                        .zone("16N")
                        .build(),
                UTMCoord.builder()
                        .easting(829575)
                        .northing(1159506)
                        .zone("16N")
                        .build());

        DatasetDTO m1 = new DatasetDTO("Dataset University of Jaen",
                "Descripción dataset 1", LocalDateTime.now(), georefBox1,
                100000, "LAZ");

        Mono<String> response = datasetEndpointsCaller.addDataset("University of Jaen", m1, HttpStatus.CONFLICT);
        StepVerifier.create(response)
                .expectNext(errMessage)
                .verifyComplete();


    }


    @Test
    public void completeTest() {

        //Add worskpace
        String workspaceName = "Cordoba";
        WorkspaceDTO workspaceDTO = new WorkspaceDTO(workspaceName, "Workspace of Cordoba", 10000);
        datasetEndpointsCaller.getWorkspaceEndpointsCaller().addWorkspace(workspaceDTO, HttpStatus.CREATED);

        String utmCoordSouthWestquery = "33N3500004750000";
        String utmCoordNorthEastquery = "33N3620004760000";

        UTMCoord utmCoordSouthWest = UTMCoord.builder().easting(350601).northing(4752329).zone("33N")
                .build();
        UTMCoord utmCoordNorthEast = UTMCoord.builder().easting(351359).northing(4758329).zone("33N")
                .build();
        GeorefBox georefBox = new GeorefBox(utmCoordSouthWest, utmCoordNorthEast);

        DatasetDTO datasetOld = new DatasetDTO("Dataset 1", "Old Dataset",
                LocalDateTime.of(2017, 1, 1, 12, 30),
                georefBox, 100000, "LAZ");

        DatasetDTO datasetNew = new DatasetDTO("Dataset 2", "Old Dataset",
                LocalDateTime.of(2020, 1, 1, 12, 30),
                georefBox, 100000, "LAZ");

        //Post two datasets that occupy the same area but with diferent moments of creation
        datasetEndpointsCaller.addDataset(workspaceName, datasetOld, HttpStatus.CREATED).subscribe();
        datasetEndpointsCaller.addDataset(workspaceName, datasetNew, HttpStatus.CREATED).subscribe();

        Flux<String> firstQueryResponse = datasetEndpointsCaller.getDatasetsInWorkspace(workspaceName,
                Optional.of(utmCoordSouthWestquery), Optional.of(utmCoordNorthEastquery),
                Optional.empty(), Optional.empty(),
                HttpStatus.OK);

        assert firstQueryResponse.count().block().equals(2L);

        LocalDateTime localDateTimeOld = LocalDateTime.of(2016, 1, 1, 12, 0);
        LocalDateTime localDateTimeMiddle = LocalDateTime.of(2018, 1, 1, 12, 0);
        LocalDateTime localDateTimeNew = LocalDateTime.of(2022, 1, 1, 12, 0);

        Flux<String> secondQueryResponse = datasetEndpointsCaller.getDatasetsInWorkspace(workspaceName,
                Optional.of(utmCoordSouthWestquery), Optional.of(utmCoordNorthEastquery),
                Optional.of(localDateTimeOld), Optional.of(localDateTimeMiddle),
                HttpStatus.OK);

        assert secondQueryResponse.count().block().equals(1L);

        Flux<String> thirdQueryResponse = datasetEndpointsCaller.getDatasetsInWorkspace(workspaceName,
                Optional.of(utmCoordSouthWestquery), Optional.of(utmCoordNorthEastquery),
                Optional.of(localDateTimeOld), Optional.of(localDateTimeNew),
                HttpStatus.OK);
        assert thirdQueryResponse.count().block().equals(2L);

        Flux<String> fourthQueryResponse = datasetEndpointsCaller.getDatasetsInWorkspace(workspaceName,
                Optional.of(utmCoordSouthWestquery), Optional.of(utmCoordNorthEastquery),
                Optional.of(localDateTimeMiddle), Optional.of(localDateTimeNew),
                HttpStatus.OK);
        assert fourthQueryResponse.count().block().equals(1L);
    }


}
