package com.codism.filestorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodismFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodismFileStorageApplication.class, args);
    }
}
