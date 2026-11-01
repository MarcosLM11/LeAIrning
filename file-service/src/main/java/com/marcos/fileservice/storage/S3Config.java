package com.marcos.fileservice.storage;

import java.net.URI;
import java.util.concurrent.CompletionException;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
class S3Config {

    @Bean(destroyMethod = "close")
    S3AsyncClient s3AsyncClient(S3Properties p) {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .forcePathStyle(true)
                .multipartEnabled(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    ApplicationRunner createBucketIfMissing(S3AsyncClient s3, S3Properties p) {
        return args -> {
            try {
                s3.headBucket(b -> b.bucket(p.bucket())).join();
            } catch (CompletionException e) {
                if (!(e.getCause() instanceof NoSuchBucketException)) {
                    throw e;
                }
                s3.createBucket(b -> b.bucket(p.bucket())).join();
            }
        };
    }
}
