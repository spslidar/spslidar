package ujaen.spslidar.utils.properties;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lastools")
@Data
@NoArgsConstructor
public class LasToolsProperties {

    private String extension;
    private String algorithm;



}
