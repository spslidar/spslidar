package ujaen.spslidar.utils.geoconverter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LongLatGeorefBox {

    LongLat southWestBottom;
    LongLat northEastTop;

    /**
     * Checks if this GeorefBox contains inside the georefbox passed as argument
     *
     * @param georefBox
     * @return
     */
    public boolean doesOverlap(LongLatGeorefBox georefBox) {

        if (georefBox.getNorthEastTop().getLongitude() < this.southWestBottom.getLongitude()) {
            return false;
        }
        if (georefBox.getSouthWestBottom().getLongitude() > this.southWestBottom.getLongitude()) {
            return false;
        }
        if (georefBox.getNorthEastTop().getLatitude() < this.southWestBottom.getLatitude()) {
            return false;
        }
        if (georefBox.getSouthWestBottom().getLatitude() > this.northEastTop.getLatitude()) {
            return false;
        }

        return true;

    }

}
