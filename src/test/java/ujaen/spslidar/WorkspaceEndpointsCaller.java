package ujaen.spslidar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ujaen.spslidar.DTOs.http.WorkspaceDTO;

@Service
public class WorkspaceEndpointsCaller {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ApplicationContext applicationContext;

    private static final String uriRoot = "http://localhost:8088/spslidar/";

    Logger logger = LoggerFactory.getLogger(WorkspaceEndpointsCaller.class);



    public Flux<WorkspaceDTO> getAllWorkspaces(){

        String uri = uriRoot+"workspaces";

        return this.webTestClient.get()
                .uri(uri)
                .exchange()
                .expectStatus().isOk()
                .returnResult(WorkspaceDTO.class)
                .getResponseBody();
    }


    public Mono<String> getWorkspaceByName(String workspaceName, HttpStatus httpStatus){
        String uri = uriRoot+"workspaces/"+workspaceName;

        return this.webTestClient.get()
                .uri(uri)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody()
                .next();
    }


    public Mono<String> addWorkspace(WorkspaceDTO workspaceDTO, HttpStatus httpStatus){
        String uri = uriRoot+"workspaces";

        return this.webTestClient.post()
                .uri(uri)
                .bodyValue(workspaceDTO)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .returnResult(String.class)
                .getResponseBody()
                .next();

    }


    public void addSimpleWorkspace() {
        WorkspaceDTO workspaceDTO = new WorkspaceDTO("University of Jaen",
                "Workspace of the University of Jaen", 10000);

        Mono<String> response =
                this.addWorkspace(workspaceDTO, HttpStatus.CREATED);

        StepVerifier
                .create(response.map(this::mapToWorkspaceDTO))
                .expectNext(workspaceDTO)
                .verifyComplete();

    }


    public WorkspaceDTO mapToWorkspaceDTO(String string) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(string, WorkspaceDTO.class);

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new WorkspaceDTO();

    }




}
