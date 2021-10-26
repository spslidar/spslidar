package ujaen.spslidar.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ujaen.spslidar.utils.properties.OctreeProperties;

import java.util.ArrayList;
import java.util.List;

@Component
public class NodeSizeDistribution {

    @Autowired
    OctreeProperties octreeProperties;


    public List<Integer> percentagesGenerator(Long numberOfPointsInOctree) {
        List<Integer> percentagesPerDepthLevel = new ArrayList<>();
        float lastValue = octreeProperties.getInitialValue();
        int numberOfPoints = Math.round(numberOfPointsInOctree * lastValue / 100);
        percentagesPerDepthLevel.add(numberOfPoints);

        for (int i = 1; i < octreeProperties.getMaxDepth(); i++) {
            lastValue *= octreeProperties.getMultFactor();
            numberOfPoints = (int) Math.min(Math.round(numberOfPointsInOctree * lastValue / 100), numberOfPointsInOctree);
            percentagesPerDepthLevel.add(i, numberOfPoints);
        }

        return percentagesPerDepthLevel;
    }
}
