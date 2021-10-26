package ujaen.spslidar.Exceptions;

public class DatasetHasDataAssociated extends RuntimeException {

    public DatasetHasDataAssociated() {
        super("Dataset already has data associated");
    }
}
