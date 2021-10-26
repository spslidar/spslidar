package ujaen.spslidar.Exceptions;

public class DatasetAlreadyExists extends RuntimeException{

    public DatasetAlreadyExists(){
        super("A dataset with this ID already exists");
    }

}

