/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ujaen.spslidar.Exceptions;

/**
 *
 * @author jabm9
 */
public class WorkspaceExistsException extends RuntimeException{
    
    public WorkspaceExistsException(){
        super("Workspace already exists");
    }
    
}
