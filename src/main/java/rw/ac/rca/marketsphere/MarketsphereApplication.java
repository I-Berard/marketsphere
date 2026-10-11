package rw.ac.rca.marketsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import rw.ac.rca.marketsphere.config.MarketSphereProperties;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class
})
@EnableConfigurationProperties(MarketSphereProperties.class)
public class MarketsphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketsphereApplication.class, args);
    }
}
