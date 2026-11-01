package com.marcos.fileservice;

import org.springframework.boot.SpringApplication;

public class TestFileServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(FileServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
