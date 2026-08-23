package lostlink.media.service.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcsConfig {

    @Bean
    @ConditionalOnProperty(name = "findora.storage.type", havingValue = "gcs")
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}