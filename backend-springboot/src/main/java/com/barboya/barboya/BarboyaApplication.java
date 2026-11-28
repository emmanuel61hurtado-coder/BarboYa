package com.barboya.barboya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BarboyaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BarboyaApplication.class, args);
    }
}