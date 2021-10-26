package ujaen.spslidar.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class GridCell {

    GeorefBox gridGeorefBox;

    List<String> datasets = new ArrayList<>();

    public GridCell(GeorefBox gridGeorefBox) {
        this.gridGeorefBox = gridGeorefBox;
    }

    public GridCell addDataset(String dataset) {
        if (!datasets.contains(dataset))
            datasets.add(dataset);
        return this;
    }

}
