package com.marcos.ingestionservice.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("s3")
record S3Properties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket
) {}