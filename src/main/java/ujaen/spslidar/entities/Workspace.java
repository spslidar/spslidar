package ujaen.spslidar.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import ujaen.spslidar.DTOs.http.WorkspaceDTO;

/**
 * Workspace class, containing one or more georreferenced point clouds
 */

@Data
@NoArgsConstructor
public class Workspace {

    private String name;
    private String description;
    private int cellSize;


    public Workspace(String name, String description, int cellSize) {
        this.name = name;
        this.description = description;
        this.cellSize = cellSize;
    }

    public Workspace(WorkspaceDTO workspaceDTO) {
        this.name = workspaceDTO.getName();
        this.description = workspaceDTO.getDescription();
        this.cellSize = workspaceDTO.getCellSize();
    }


}
