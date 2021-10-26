package ujaen.spslidar.Exceptions;

public class DifferentUTMZone extends RuntimeException{

    public DifferentUTMZone(){
        super("This bounding box must have coordinates expressed in the same UTM Zone");
    }


}
