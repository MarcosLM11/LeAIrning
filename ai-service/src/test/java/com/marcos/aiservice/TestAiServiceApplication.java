package com.marcos.aiservice;

import org.springframework.boot.SpringApplication;

public class TestAiServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(AiServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
