package ujaen.spslidar.Exceptions;

public class ElementNotFound extends RuntimeException {

    public ElementNotFound(){
        super("The workspace or dataset specified does not exist");
    }

}
