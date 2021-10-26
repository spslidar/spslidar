package ujaen.spslidar.utils.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
@Data
@NoArgsConstructor
public class FileStorageProperties {

    private String uploadDir;

    private String mergeDir;


}
