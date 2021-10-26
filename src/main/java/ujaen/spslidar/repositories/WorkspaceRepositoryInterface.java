/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.Workspace;

/**
 * Repository interface for the workspace entity
 */
public interface WorkspaceRepositoryInterface {

    /**
     * Find all workspaces
     * @return all workspaces available
     */
    Flux<Workspace> findAll();

    /**
     * Find workspace by its name
     * @param workspaceName name of the workspace
     * @return Mono with the workspace if it exists, otherwise Mono.empty()
     */
    Mono<Workspace> findByName(String workspaceName);

    /**
     * Check if a workspace exists by its name
     * @param workspaceName name of the workspace
     * @return boolean with the result
     */
    Mono<Boolean> existsByName(String workspaceName);

    /**
     * Save a new workspace in the system
     * @param workspace workspace to add
     * @return metadata of the same workspace inserted
     */
    Mono<Workspace> save(Workspace workspace);


}
