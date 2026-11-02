package com.marcos.fileservice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }

    @Bean
    GenericContainer<?> s3Container() {
        return new GenericContainer<>(DockerImageName.parse("rustfs/rustfs:1.0.4"))
                .withEnv("RUSTFS_VOLUMES", "/data")
                .withEnv("RUSTFS_ACCESS_KEY", "rustfsadmin")
                .withEnv("RUSTFS_SECRET_KEY", "rustfsadmin")
                .withExposedPorts(9000)
                .waitingFor(Wait.forListeningPort());
    }

    @Bean
    DynamicPropertyRegistrar s3Endpoint(@Qualifier("s3Container") GenericContainer<?> s3) {
        return registry -> registry.add("s3.endpoint", () -> "http://" + s3.getHost() + ":" + s3.getMappedPort(9000));
    }

}