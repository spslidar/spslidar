/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author l3pc126
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeorefBox {

    private UTMCoord southWestBottom;

    private UTMCoord northEastTop;


    public GeorefBox getSubRegions(int regionIndex, String UTMZone) {

        double mediumX = southWestBottom.getEasting() + ((getNorthEastTop().getEasting() - getSouthWestBottom().getEasting()) / 2);
        double mediumY = southWestBottom.getNorthing() + ((getNorthEastTop().getNorthing() - getSouthWestBottom().getNorthing()) / 2);
        double mediumZ = southWestBottom.getHeight() + ((getNorthEastTop().getHeight() - getSouthWestBottom().getHeight()) / 2);

        UTMCoord sw;
        UTMCoord ne;

        switch (regionIndex) {
            case 0:
                sw = new UTMCoord(southWestBottom.getEasting(), southWestBottom.getNorthing(), UTMZone, southWestBottom.getHeight());
                ne = new UTMCoord(mediumX, mediumY, UTMZone, mediumZ);
                return new GeorefBox(sw, ne);

            case 1:
                sw = new UTMCoord(mediumX, southWestBottom.getNorthing(), UTMZone, southWestBottom.getHeight());
                ne = new UTMCoord(northEastTop.getEasting(), mediumY, UTMZone, mediumZ);
                return new GeorefBox(sw, ne);

            case 2:
                sw = new UTMCoord(southWestBottom.getEasting(), mediumY, UTMZone, southWestBottom.getHeight());
                ne = new UTMCoord(mediumX, northEastTop.getNorthing(), UTMZone, mediumZ);
                return new GeorefBox(sw, ne);

            case 3:
                sw = new UTMCoord(mediumX, mediumY, UTMZone, southWestBottom.getHeight());
                ne = new UTMCoord(northEastTop.getEasting(), northEastTop.getNorthing(), UTMZone, mediumZ);
                return new GeorefBox(sw, ne);

            case 4:

                sw = new UTMCoord(southWestBottom.getEasting(), southWestBottom.getNorthing(), UTMZone, mediumZ);
                ne = new UTMCoord(mediumX, mediumY, UTMZone, northEastTop.getHeight());
                return new GeorefBox(sw, ne);

            case 5:
                sw = new UTMCoord(mediumX, southWestBottom.getNorthing(), UTMZone, mediumZ);
                ne = new UTMCoord(northEastTop.getEasting(), mediumY, UTMZone, northEastTop.getHeight());
                return new GeorefBox(sw, ne);

            case 6:
                sw = new UTMCoord(southWestBottom.getEasting(), mediumY, UTMZone, mediumZ);
                ne = new UTMCoord(mediumX, northEastTop.getNorthing(), UTMZone, northEastTop.getHeight());
                return new GeorefBox(sw, ne);

            case 7:
                sw = new UTMCoord(mediumX, mediumY, UTMZone, mediumZ);
                ne = new UTMCoord(northEastTop.getEasting(), northEastTop.getNorthing(), UTMZone, northEastTop.getHeight());
                return new GeorefBox(sw, ne);

        }

        return null;

    }


    /**
     * Checks if this GeorefBox contains inside the georefbox passed as argument
     *
     * @param georefBox
     * @return
     */
    public boolean doesOverlap(GeorefBox georefBox) {

        if (georefBox.getNorthEastTop().getEasting() < this.southWestBottom.getEasting()) {
            return false;
        }
        if (georefBox.getSouthWestBottom().getEasting() > this.southWestBottom.getEasting()) {
            return false;
        }
        if (georefBox.getNorthEastTop().getNorthing() < this.southWestBottom.getNorthing()) {
            return false;
        }
        if (georefBox.getSouthWestBottom().getNorthing() > this.northEastTop.getNorthing()) {
            return false;
        }

        return true;

    }

    /**
     * Returns true if the georefbox passed fits
     * completely inside the original georefbox
     *
     * @param georefBox
     * @return
     */
    public boolean containsGeorefBox(GeorefBox georefBox) {

        if (this.southWestBottom.getEasting() <= georefBox.getSouthWestBottom().getEasting()
                && this.southWestBottom.getNorthing() <= georefBox.getSouthWestBottom().getNorthing()
                && this.northEastTop.getEasting() > georefBox.getNorthEastTop().getEasting()
                && this.northEastTop.getNorthing() > georefBox.getNorthEastTop().getNorthing()) {
            return true;
        }
        return false;
    }

    /**
     * Returns a string identifier of this geoRefBox composed by the 2D max and mins
     *
     * @return
     */
    public String georefBox2DIdentifier() {

        return this.getSouthWestBottom().getEasting() + "_" +
                this.getSouthWestBottom().getNorthing() + "_" +
                this.getNorthEastTop().getEasting() + "_" +
                this.getNorthEastTop().getNorthing();

    }



}
