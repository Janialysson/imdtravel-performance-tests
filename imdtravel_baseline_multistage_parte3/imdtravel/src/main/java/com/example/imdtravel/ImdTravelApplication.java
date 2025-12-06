package com.example.imdtravel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ImdTravelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImdTravelApplication.class, args);
    }
}