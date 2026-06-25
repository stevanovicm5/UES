package com.app.godo.configurations;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(
            @Value("${app.storage.minio.endpoint}") String endpoint,
            @Value("${app.storage.minio.access-key}") String accessKey,
            @Value("${app.storage.minio.secret-key}") String secretKey) {

        log.info("Initializing MinIO client with endpoint: {}", endpoint);
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public MinioClientProperties minioProperties(
            @Value("${app.storage.minio.endpoint}") String endpoint,
            @Value("${app.storage.minio.public-url}") String publicUrl,
            @Value("${app.storage.minio.bucket}") String bucket) {
        return new MinioClientProperties(endpoint, publicUrl, bucket);
    }
}
