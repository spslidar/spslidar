package ujaen.spslidar.Exceptions;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException() {
        super("Unknown workspace");
    }
}
