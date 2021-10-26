package ujaen.spslidar.services.tools;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.GeorefBox;

@Service
public interface LazReaderInterface {

    /**
     * Returns the a Georefbox that represent the minimum and maximum limits of
     * the point cloud associated to the file passed by argument
     * @param pathLazFile
     * @return
     */
    public Mono<GeorefBox> getGeorefBox(String pathLazFile);


    /**
     * Returns the number of points that the point cloud associated to the file
     * passed by argument has
     * @param pathLazFile
     * @return
     */
    public Mono<Long> getNumberOfPoints(String pathLazFile);



}
