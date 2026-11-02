package com.marcos.ingestionservice;

import org.springframework.boot.SpringApplication;

public class TestIngestionServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(IngestionServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
