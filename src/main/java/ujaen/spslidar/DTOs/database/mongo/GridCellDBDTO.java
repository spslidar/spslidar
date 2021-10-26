package ujaen.spslidar.DTOs.database.mongo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.GridCell;

import java.util.List;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridCellDBDTO {

    @Id
    GeorefBox bbox;

    String UTMZone;

    List<String> datasets;

    public GridCellDBDTO(GridCell gridCell){
        this.UTMZone = gridCell.getGridGeorefBox().getSouthWestBottom().getZone();
        this.datasets = gridCell.getDatasets();
        this.bbox = gridCell.getGridGeorefBox();
    }

    public GridCell gridFromDTO(){
        return new GridCell(bbox, datasets);
    }

}
