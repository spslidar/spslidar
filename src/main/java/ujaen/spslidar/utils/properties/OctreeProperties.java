package ujaen.spslidar.utils.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "octree")
@Data
@NoArgsConstructor
public class OctreeProperties {

    private boolean linealDistribution;
    private float multFactor;
    private float initialValue;
    private int maxDepth;
}
