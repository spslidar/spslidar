package ujaen.spslidar.DTOs.database.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.entities.GeorefBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetDBDTO {

    @Id
    private String datasetName;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;

    private GeorefBox bbox;

    private int dataBlockSize;

    private String dataBlockFormat;

    private Map<String, List<GeorefBox>> gridsAssociated = new HashMap<>();

    private List<String> file = new ArrayList<>();


    public DatasetDBDTO(Dataset dataset) {
        this.datasetName = dataset.getDatasetName();
        this.description = dataset.getDescription();
        this.date = dataset.getDate();
        this.bbox = dataset.getBbox();
        this.dataBlockSize = dataset.getDataBlockSize();
        this.dataBlockFormat = dataset.getDataBlockFormat();
        this.gridsAssociated = dataset.getRootDatablocks();
        this.file = dataset.getFiles();
    }

    public Dataset datasetFromDTO() {
        return new Dataset(datasetName, description, date, bbox,
                dataBlockSize, dataBlockFormat, gridsAssociated, file);

    }

}
