package ujaen.spslidar.Exceptions;

public class NoUTMZoneInFile extends RuntimeException {

    public NoUTMZoneInFile() {
        super("No UTM Zone found in file. Make sure to add a VLR to the file specifying" +
                "the UTM Zone in which the coordinates are located");
    }
}
