package ujaen.spslidar.utils.geoconverter;

import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.UTMCoord;

import java.util.ArrayList;
import java.util.List;

public class GridAllocator {

    public static List<GeorefBox> allocateDataset(String UTMZone, GeorefBox baseGeorefBox, int localGridSize) {

        List<GeorefBox> grids = new ArrayList<>();

        UTMCoord roundedSW = UTMCoord.builder()
                .easting(Math.floor(baseGeorefBox.getSouthWestBottom().getEasting() / localGridSize) * localGridSize)
                .northing(Math.floor(baseGeorefBox.getSouthWestBottom().getNorthing() / localGridSize) * localGridSize)
                .zone(UTMZone)
                .build();

        UTMCoord roundedNE = UTMCoord.builder()
                .easting(Math.ceil(baseGeorefBox.getNorthEastTop().getEasting() / localGridSize) * localGridSize)
                .northing(Math.ceil(baseGeorefBox.getNorthEastTop().getNorthing() / localGridSize) * localGridSize)
                .zone(UTMZone)
                .build();

        for (double i = roundedSW.getEasting(); i < roundedNE.getEasting(); i += localGridSize) {
            for (double j = roundedSW.getNorthing(); j < roundedNE.getNorthing(); j += localGridSize) {
                UTMCoord sw = UTMCoord.builder().easting(i).northing(j).zone(UTMZone).build();
                UTMCoord ne = UTMCoord.builder().easting(i + localGridSize).northing(j + localGridSize).zone(UTMZone).build();
                grids.add(new GeorefBox(sw, ne));
            }

        }
        return grids;
    }
}
