package ujaen.spslidar.entities;


import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;

/**
 * UTM Coordinate class
 */
@Data
@Builder
public class UTMCoord {

    @NonNull
    private double easting;

    @NonNull
    private double northing;

    @NonNull
    private String zone;

    private double height;

    /**
     * Compares this coordinate with another one
     * Use this method if this coordinate is acting as the south west point of a bounding box
     * @param utmCoord
     * @return true if the passed coordinate is more to the east and the north than this, false otherwise
     */
    public boolean compareSouthWest(UTMCoord utmCoord){
        if(utmCoord.getEasting() >= this.easting && utmCoord.getNorthing() >= this.northing)
            return true;

        return false;

    }


    /**
     * * Compares this coordinate with another one
     * Use this method if this coordinate is acting as the north east point of a bounding box
     * @param utmCoord
     * @return
     */
    public boolean compareNorthEast(UTMCoord utmCoord){
        if(utmCoord.getEasting() > this.easting && utmCoord.getNorthing() > this.northing)
            return false;

        return true;
    }


    /**
     * Build a UTMCoord object from a String that follows the pattern ZZREE..eNNN..N, being ZZ the zone code, R the region,
     * EE..E the easting and NNN..N the northing
     *
     * @param coord string to parse
     * @return UTMCoord object
     */
    public static UTMCoord parseUTMCoord(String coord) {

        //Solves problems with decimal-like string coords
        String cleanCoord = coord.replaceAll("\\.", "");

        String UTMZone = cleanCoord.substring(0, 3);
        int resolutionLenghtOfCoordinates = (cleanCoord.length() - UTMZone.length())/2;
        int eastingNorthingDelimitator = UTMZone.length() + resolutionLenghtOfCoordinates;
        double resolutionToMetersMultiplier = Math.pow(10, 6 - resolutionLenghtOfCoordinates);


        return UTMCoord.builder()
                .easting(Double.valueOf(cleanCoord.substring(3, eastingNorthingDelimitator)) * resolutionToMetersMultiplier)
                .northing(Double.valueOf(cleanCoord.substring(eastingNorthingDelimitator)) * resolutionToMetersMultiplier)
                .zone(UTMZone)
                .build();

    }


    /**
     * Default UTMCoord for a northEast coordinate
     * @return
     */
    public static UTMCoord defaultNorthEast(){
        return UTMCoord.builder()
                .easting(Double.MAX_VALUE)
                .northing(Double.MAX_VALUE)
                .zone("60X")
                .build();
    }


    /**
     * Default UTMCoord for a southWest coordinate
     * @return
     */
    public static UTMCoord defaultSouthWest(){
        return UTMCoord.builder()
                .easting(Double.MIN_VALUE)
                .northing(Double.MIN_VALUE)
                .zone("00C")
                .build();

    }




}
