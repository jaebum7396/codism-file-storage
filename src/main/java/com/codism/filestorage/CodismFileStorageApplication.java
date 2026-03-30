package com.codism.filestorage;

import ai.codism.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import(GlobalExceptionHandler.class)
public class CodismFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodismFileStorageApplication.class, args);
    }
}
