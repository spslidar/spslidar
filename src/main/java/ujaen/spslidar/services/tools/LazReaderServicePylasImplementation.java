package ujaen.spslidar.services.tools;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ujaen.spslidar.entities.GeorefBox;
import ujaen.spslidar.entities.UTMCoord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LazReaderServicePylasImplementation implements LazReaderInterface {

    Logger logger = LoggerFactory.getLogger(LazReaderServicePylasImplementation.class);

    @Autowired
    LasToolsService lasToolsService;

    String directory = System.getProperty("user.dir") + "\\python-utils";
    String pathToNumberOfPointsScript = "numberOfPoints.py";
    String pathToGetGeorefBoxScript = "bboxLimits.py";

    @Override
    public Mono<GeorefBox> getGeorefBox(String pathLazFile) {

        String arguments[] = {"python", pathToGetGeorefBoxScript, pathLazFile};
        ProcessBuilder pb = new ProcessBuilder(arguments);
        pb.directory(new File(directory));
        pb.redirectErrorStream(true);
        Mono<String> UTMZoneMono = (Mono<String>) lasToolsService.getUTMZone(pathLazFile);

        try {
            return Mono.just(IOUtils.toString(pb.start().getInputStream(),
                    StandardCharsets.UTF_8))
                    .zipWith(UTMZoneMono)
                    .map(tuple -> {
                        //T1 -> Bounding box limits
                        //T2 -> UTM Zone
                        String[] substrings = tuple.getT1().split("\n");

                        UTMCoord southWest = UTMCoord.builder()
                                .easting(Double.valueOf(substrings[0]))
                                .northing(Double.valueOf(substrings[1]))
                                .height(Double.valueOf(substrings[2]))
                                .zone(tuple.getT2())
                                .build();

                        UTMCoord northEast = UTMCoord.builder()
                                .easting(Double.valueOf(substrings[3]))
                                .northing(Double.valueOf(substrings[4]))
                                .height(Double.valueOf(substrings[5]))
                                .zone(tuple.getT2())
                                .build();

                        GeorefBox georefBox = new GeorefBox(southWest, northEast);
                        logger.info(georefBox.toString());
                        return new GeorefBox(southWest, northEast);
                    });
        } catch (IOException ioException) {
            return Mono.error(ioException);
        }


    }

    @Override
    public Mono<Long> getNumberOfPoints(String pathLazFile) {

        String arguments[] = {"python", pathToNumberOfPointsScript, pathLazFile};
        ProcessBuilder pb = new ProcessBuilder(arguments);
        pb.directory(new File(directory));
        pb.redirectErrorStream(true);

        return Mono.fromCallable(() -> {
            Process process = pb.start();
            String ret = IOUtils.toString(process.getInputStream(),
                    StandardCharsets.UTF_8).trim();
            process.waitFor();

            return ret;
        }).map(s -> {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException n) {
                logger.info("File does not contain points: " + pathLazFile);
                return 0L;
            }
        });


    }
}



