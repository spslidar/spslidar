package ujaen.spslidar.DTOs.database.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ujaen.spslidar.entities.Workspace;

@Document("workspaces")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceDBDTO {

    @Id
    private String workspaceName;
    private String description;
    private int cellSize;


    public WorkspaceDBDTO(Workspace ws){
        this.workspaceName = ws.getName();
        this.description = ws.getDescription();
        this.cellSize = ws.getCellSize();
    }

    public Workspace workspaceFromDTO(){
        return new Workspace(workspaceName, description, cellSize);
    }


}
