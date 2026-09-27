package rw.ac.rca.cloudsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import rw.ac.rca.cloudsphere.config.CloudSphereProperties;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class
})
@EnableConfigurationProperties(CloudSphereProperties.class)
public class CloudsphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudsphereApplication.class, args);
    }
}
