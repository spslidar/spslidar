package ujaen.spslidar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatasetDTO;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.UTMCoord;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DatasetEndpointsCaller {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ApplicationContext applicationContext;

    @Getter
    @Autowired
    WorkspaceEndpointsCaller workspaceEndpointsCaller;

    @Autowired
    ObjectMapper mapper;

    private static final String uriRoot = "http://localhost:8080/spslidar/workspaces/";

    Logger logger = LoggerFactory.getLogger(DatasetEndpointsCaller.class);


    /**
     * Query datasets from a specific workspace
     *
     * @param workspaceName
     * @param southWest     optional
     * @param northEast     optional
     * @param fromDate      optional
     * @param toDate        optional
     * @param httpStatus
     * @return
     */
    public Flux<String> getDatasetsInWorkspace(String workspaceName,
                                               Optional<String> southWest, Optional<String> northEast,
                                               Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
                                               HttpStatus httpStatus) {

        String uri = uriRoot + workspaceName + "/datasets";

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(uri)
                .queryParamIfPresent("sw_coord", southWest)
                .queryParamIfPresent("ne_coord", northEast)
                .queryParamIfPresent("from_date", fromDate)
                .queryParamIfPresent("to_date", toDate)
                .build();

        return this.webTestClient
                .get()
                .uri(uriComponents.toUriString())
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody();

    }


    /**
     * Query a specific dataset by its name and the workspace it belongs to
     *
     * @param workspaceName
     * @param datasetName
     * @param httpStatus
     * @return
     */
    public Mono<String> getDatasetByName(String workspaceName, String datasetName, HttpStatus httpStatus) {

        String uri = uriRoot + workspaceName + "/datasets/" + datasetName;

        return this.webTestClient
                .get()
                .uri(uri)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody()
                .next();

    }

    /**
     * Adds a new dataset to the system.
     * @param workspaceName
     * @param datasetDTO
     * @param httpStatus
     * @return
     */
    public Mono<String> addDataset(String workspaceName, DatasetDTO datasetDTO, HttpStatus httpStatus){

        String uri = uriRoot + workspaceName + "/datasets";

        return this.webTestClient
                .post()
                .uri(uri)
                .bodyValue(datasetDTO)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody()
                .next();

    }

    public void addSimpleDataset() {

        workspaceEndpointsCaller.addSimpleWorkspace();

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

        DatasetDTO response = this.addDataset("University of Jaen",m1, HttpStatus.CREATED)
                .map(this::mapToDatasetDTO)
                .block();

        assert response.getName().equals(m1.getName());
        assert response.getDescription().equals(m1.getDescription());
        assert response.getBoundingBox().equals(m1.getBoundingBox());

    }

    public DatasetDTO mapToDatasetDTO(String string) {
        try {
            return mapper.readValue(string, DatasetDTO.class);

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DatasetDTO();

    }



}
