package com.guftagu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuftaguBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuftaguBackendApplication.class, args);
    }

}
