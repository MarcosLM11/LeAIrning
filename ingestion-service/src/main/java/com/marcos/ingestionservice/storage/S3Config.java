package com.marcos.ingestionservice.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
class S3Config {

    @Bean(destroyMethod = "close")
    S3Client s3Client(S3Properties p) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .forcePathStyle(true)
                .build();
    }
}
