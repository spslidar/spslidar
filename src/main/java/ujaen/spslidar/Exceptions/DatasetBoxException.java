package ujaen.spslidar.Exceptions;

public class DatasetBoxException extends RuntimeException{

    public DatasetBoxException(){
        super("Dataset box is larger than the grid size");
    }

}
