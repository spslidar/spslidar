package ujaen.spslidar.DTOs.http;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import ujaen.spslidar.entities.Dataset;
import ujaen.spslidar.entities.GeorefBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetDTO {

    @NonNull
    private String name;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateOfAcquisition;

    private GeorefBox boundingBox;

    private int dataBlockSize;

    private String dataBlockFormat;

    private List<GeorefBox> rootDatablocks = new ArrayList<>();

    public DatasetDTO(Dataset m) {

        this.name = m.getDatasetName();
        this.description = m.getDescription();
        this.dateOfAcquisition = m.getDate();
        this.boundingBox = m.getBbox();
        this.dataBlockSize = m.getDataBlockSize();
        this.dataBlockFormat = m.getDataBlockFormat();

        for (String key : m.getRootDatablocks().keySet()) {
            rootDatablocks.addAll(m.getRootDatablocks().get(key));
        }

    }

    public DatasetDTO(String name, String description, LocalDateTime date, GeorefBox boundingBox,
                      int dataBlockSize, String dataBlockFormat) {

        this.name = name;
        this.description = description;
        this.dateOfAcquisition = date;
        this.boundingBox = boundingBox;
        this.dataBlockSize = dataBlockSize;
        this.dataBlockFormat = dataBlockFormat;
    }
}
