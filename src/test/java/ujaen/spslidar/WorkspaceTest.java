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
import ujaen.spslidar.DTOs.http.WorkspaceDTO;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "600000000")
public class WorkspaceTest {

    @Autowired
    WorkspaceEndpointsCaller workspaceEndpointsCaller;

    @Test
    public void getAllWorkspacesTest() {
        workspaceEndpointsCaller.addSimpleWorkspace();
        Flux<WorkspaceDTO> workspaceDTOFlux = workspaceEndpointsCaller.getAllWorkspaces();

        StepVerifier.create(workspaceDTOFlux.count())
                .expectNextCount(1L)
                .verifyComplete();
    }

    @Test
    public void getWorkspaceByNameTest200() {
        workspaceEndpointsCaller.addSimpleWorkspace();
        String workspaceName = "University of Jaen";
        Mono<String> workspaceDTOMono =
                workspaceEndpointsCaller.getWorkspaceByName(workspaceName, HttpStatus.OK);

        StepVerifier
                .create(workspaceDTOMono
                        .map(workspaceEndpointsCaller::mapToWorkspaceDTO)
                        .map(WorkspaceDTO::getName))
                .expectNext(workspaceName)
                .verifyComplete();
    }

    @Test
    public void getWorkspaceByNameTest404() {
        workspaceEndpointsCaller.addSimpleWorkspace();
        String workspaceName = "Randome workspace name";
        String errorExceptionMessage = "No workspace with this name has been found";

        Mono<String> responseMessage =
                workspaceEndpointsCaller.getWorkspaceByName(workspaceName, HttpStatus.NOT_FOUND);

        StepVerifier
                .create(responseMessage)
                .expectNext(errorExceptionMessage)
                .verifyComplete();
    }

    @Test
    public void addWorkspaceTest201() {
        workspaceEndpointsCaller.addSimpleWorkspace();

    }

    @Test
    public void addWorkspaceTest409() {
        workspaceEndpointsCaller.addSimpleWorkspace();
        WorkspaceDTO workspaceDTO = new WorkspaceDTO("University of Jaen",
                "Workspace of the University of Jaen", 10000);
        String errorExceptionMessage = "A workspace with the specified name already exists in the system";


        Mono<String> responseMessage =
                workspaceEndpointsCaller.addWorkspace(workspaceDTO, HttpStatus.CONFLICT);

        StepVerifier
                .create(responseMessage)
                .expectNext(errorExceptionMessage)
                .verifyComplete();
    }






}
