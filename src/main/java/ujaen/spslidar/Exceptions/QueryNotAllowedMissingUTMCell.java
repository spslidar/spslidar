package ujaen.spslidar.Exceptions;

public class QueryNotAllowedMissingUTMCell extends RuntimeException{

    public QueryNotAllowedMissingUTMCell(){
        super("You must also specify a UTMCell");
    }

}
