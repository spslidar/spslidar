package ujaen.spslidar.repositories;

import org.springframework.stereotype.Service;

@Service
public class CollectionsManager {

    public static String cleanCollectionName(String workspaceName){
        String cleanedWorkspaceName = workspaceName
                .replaceAll("\\$", "")
                .replaceAll("\\.","");

        return cleanedWorkspaceName;
    }

    public static String cleanCollectionNameCassandra(String workspaceName){

        return workspaceName.replaceAll("\\$", "")
                .replaceAll("\\.","")
                .replaceAll(" ","");
    }

}
