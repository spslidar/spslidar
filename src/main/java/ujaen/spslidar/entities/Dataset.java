/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import ujaen.spslidar.DTOs.http.DatasetDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Georeferenced dataset (normally a point cloud).
 * Datasets can overlap in space and time (evolution of a dataset over time or several datasets of the same site in different resolutions).
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dataset {

    private String datasetName;

    private String workspaceName;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;

    private GeorefBox bbox;

    private int dataBlockSize;

    private String dataBlockFormat;

    private Map<String, List<GeorefBox>> rootDatablocks = new HashMap<>();

    private List<String> files = new ArrayList<>();
    
    public Dataset(DatasetDTO datasetDTO) {
        this.datasetName = datasetDTO.getName();
        this.description = datasetDTO.getDescription();
        this.date = datasetDTO.getDateOfAcquisition();
        this.bbox = datasetDTO.getBoundingBox();
        this.dataBlockSize = datasetDTO.getDataBlockSize();
        this.dataBlockFormat = datasetDTO.getDataBlockFormat();
        this.files = new ArrayList<>();
    }


    public Dataset(String datasetName, String description, LocalDateTime date, GeorefBox bbox, int dataBlockSize,
                   String dataBlockFormat, Map<String, List<GeorefBox>> rootDatablocks, List<String> files) {

        this.workspaceName = "";
        this.datasetName = datasetName;
        this.description = description;
        this.date = date;
        this.bbox = bbox;
        this.dataBlockSize = dataBlockSize;
        this.dataBlockFormat = dataBlockFormat;
        this.rootDatablocks = rootDatablocks;
        this.files = files;
    }

    public void addGrid(GeorefBox georefBox) {

        if (!this.rootDatablocks.containsKey(georefBox.getSouthWestBottom().getZone())) {
            this.rootDatablocks.put(georefBox.getSouthWestBottom().getZone(), new ArrayList<>());
        }
        this.rootDatablocks.get(georefBox.getSouthWestBottom().getZone()).add(georefBox);
    }

    public List<GeorefBox> getGridsAssociatedAsList() {
        return this.getRootDatablocks().entrySet().stream()
                .map(Map.Entry::getValue).flatMap(List::stream).collect(Collectors.toList());

    }
}
