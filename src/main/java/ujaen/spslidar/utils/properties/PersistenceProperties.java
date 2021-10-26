package ujaen.spslidar.utils.properties;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "persistence")
@Data
@NoArgsConstructor
public class PersistenceProperties {

    String workspaceRepository;
    String datasetRepository;
    String gridRepository;
    String datablockRepository;

}
