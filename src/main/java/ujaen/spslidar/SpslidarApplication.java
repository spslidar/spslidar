package ujaen.spslidar;


import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ujaen.spslidar.utils.properties.FileStorageProperties;
import ujaen.spslidar.utils.properties.LasToolsProperties;
import ujaen.spslidar.utils.properties.OctreeProperties;

@SpringBootApplication
@EnableConfigurationProperties(
        {FileStorageProperties.class, LasToolsProperties.class,
        OctreeProperties.class})
public class SpslidarApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpslidarApplication.class, args);
    }

    /**
     * Micrometer initialization
     * @param registry
     * @return
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry){
        return new TimedAspect(registry);
    }

}
