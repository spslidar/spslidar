package ujaen.spslidar.DTOs.http;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ujaen.spslidar.entities.Workspace;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceDTO {

    String name;
    String description;
    int cellSize;


    public WorkspaceDTO(Workspace ws){
        this.name = ws.getName();
        this.description = ws.getDescription();
        this.cellSize = ws.getCellSize();
    }


}
