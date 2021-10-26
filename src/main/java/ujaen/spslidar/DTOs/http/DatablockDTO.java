package ujaen.spslidar.DTOs.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ujaen.spslidar.DTOs.database.mongo.DatablockDBDTO;
import ujaen.spslidar.entities.AbstractDatablock;
import ujaen.spslidar.entities.GeorefBox;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatablockDTO {

    int id;
    GeorefBox bbox;
    long size;
    List<Integer> children;
    GeorefBox cell;


    public DatablockDTO(AbstractDatablock datablock){
        this.id = datablock.getId();
        this.bbox = datablock.getGeorefBox();
        this.size = datablock.getNumberOfPoints();
        this.children = datablock.getChildren();
        this.cell = datablock.getUTMZoneLocalGrid();

    }

    public DatablockDTO(DatablockDBDTO datablockDBDTO){

        this.id = datablockDBDTO.getNode();
        this.bbox = datablockDBDTO.getBbox();
        this.size = datablockDBDTO.getNumberOfPoints();
        this.children = datablockDBDTO.getChildren();
        this.cell = datablockDBDTO.getCell();
    }




}
