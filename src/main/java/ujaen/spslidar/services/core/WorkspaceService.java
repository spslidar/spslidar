/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.services.core;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.DTOs.http.WorkspaceDTO;
import ujaen.spslidar.entities.Workspace;
import ujaen.spslidar.repositories.WorkspaceRepositoryInterface;


@Service
public class WorkspaceService {

    private WorkspaceRepositoryInterface workspaceRepositoryInterface;

    public WorkspaceService(
            WorkspaceRepositoryInterface workspaceRepositoryInterface) {
        this.workspaceRepositoryInterface = workspaceRepositoryInterface;
    }

    /**
     * Retrieve all the workspaces
     * @return Flux of WorkspaceDTO
     */
    public Flux<WorkspaceDTO> getWorkspaces() {
        return workspaceRepositoryInterface.findAll()
                .map(WorkspaceDTO::new)
                .log();
    }

    /**
     * Retrieve a workspace identified by its name
     * @param name name of the workspace
     * @return Mono of WorkspaceDTO or Mono empty
     */
    public Mono<WorkspaceDTO> getWorkspace(String name) {
        return workspaceRepositoryInterface
                .findByName(name)
                .map(WorkspaceDTO::new)
                .log();
    }

    /**
     * Adds a new workspace to the system
     * @param workspaceDTO information of the workspace to be added
     * @return Mono of WorkspaceDTO or Mono empty
     */
    public Mono<WorkspaceDTO> addWorkspace(WorkspaceDTO workspaceDTO) {

        return workspaceRepositoryInterface
                .existsByName(workspaceDTO.getName())
                .flatMap(aBoolean -> aBoolean ? Mono.empty() : workspaceRepositoryInterface
                        .save(new Workspace(workspaceDTO))
                        .map(WorkspaceDTO::new))
                .log();
    }


    /**
     * Returns a boolean indicating the existance of the workspace in the database
     * @param workspaceName
     * @return Mono Boolean with the result
     */
    public Mono<Boolean> workspaceExists(String workspaceName) {
        return workspaceRepositoryInterface.existsByName(workspaceName)
                .log();

    }

}
