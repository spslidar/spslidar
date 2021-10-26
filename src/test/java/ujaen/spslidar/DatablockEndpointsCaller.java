package ujaen.spslidar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.DatablockDTO;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.Optional;

@Service
public class DatablockEndpointsCaller {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Getter
    DatasetEndpointsCaller datasetEndpointsCaller;

    @Autowired
    ObjectMapper mapper;

    private static final String uriRoot = "http://localhost:8080/spslidar/workspaces/";

    Logger logger = LoggerFactory.getLogger(DatablockEndpointsCaller.class);


    /**
     * Retrieve a datalbock
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @param southWest
     * @param northEast
     * @param httpStatus
     * @return
     */
    public Flux<String> getDatablock(String workspaceName, String datasetName, int id,
                                     Optional<String> southWest, Optional<String> northEast,
                                     HttpStatus httpStatus) {

        String uri = uriRoot + workspaceName + "/datasets/" + datasetName + "/datablocks/" + id;

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(uri)
                .queryParamIfPresent("southWest", southWest)
                .queryParamIfPresent("northEast", northEast)
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
     * Adds new dataset
     *
     * @param workspaceName
     * @param datasetName
     * @param datasetPath
     * @param httpStatus
     * @return
     */
    public Mono<String> addDataset(String workspaceName, String datasetName, String datasetPath, HttpStatus httpStatus) throws IOException {
        String uri = uriRoot + workspaceName + "/datasets/" + datasetName + "/data";
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        Flux<Resource> resourceFlux = Flux.fromStream(Files.walk(Paths.get(datasetPath)))
                .filter(Files::isRegularFile)
                .map(file -> {
                    Resource resource = resourceLoader.getResource("file:" + file.toAbsolutePath().toString());
                    try {
                        System.out.println("Size of file: " + resource.contentLength());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return resource;
                })
                .cast(Resource.class);

        builder.asyncPart("files", (Publisher) resourceFlux, Resource.class).contentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        return this.webTestClient
                .put()
                .uri(uri)
                .bodyValue(multipartBody)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody()
                .next();
    }


    /**
     * Retrieve the laz file associated to a datablock
     *
     * @param workspaceName
     * @param datasetName
     * @param id
     * @param southWest
     * @param northEast
     * @param httpStatus
     * @param writeFile
     * @return
     */
    public void getDatablockContent(String workspaceName, String datasetName, int id,
                                    String southWest, String northEast,
                                    HttpStatus httpStatus, Boolean writeFile) {


        String uri = uriRoot + workspaceName + "/datasets/" + datasetName + "/datablocks/" + id + "/data";

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(uri)
                .queryParam("sw_coord", southWest)
                .queryParam("ne_coord", northEast)
                .build();

        Flux<DataBuffer> dataBufferFlux = this.webTestClient
                .get()
                .uri(uriComponents.toUriString())
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(DataBuffer.class)
                .getResponseBody();

        writeFile(writeFile, dataBufferFlux);
    }


    public void getCompleteDataset(String workspaceName, String datasetName, HttpStatus httpStatus, Boolean writeFile) {

        String uri = uriRoot + workspaceName + "/datasets/" + datasetName + "/data";

        Flux<DataBuffer> dataBufferFlux = this.webTestClient
                .get()
                .uri(uri)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(DataBuffer.class)
                .getResponseBody();

        writeFile(writeFile, dataBufferFlux);

    }


    public DatablockDTO mapToDatablockDTO(String string) {
        try {
            return mapper.readValue(string, DatablockDTO.class);

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DatablockDTO();

    }


    private void writeFile(Boolean writeFile, Flux<DataBuffer> dataBufferFlux) {
        if (writeFile) {
            String path = "test.laz";
            try {
                AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(
                        Path.of(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                DataBufferUtils.write(dataBufferFlux, asynchronousFileChannel)
                        .doOnComplete(() -> {
                            try {
                                asynchronousFileChannel.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }).subscribe();

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            assert dataBufferFlux.count().block() > 0L;
        }
    }

}
