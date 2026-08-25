package com.example.lostandfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LostAndFoundApplication {
    public static void main(String[] args) {
        SpringApplication.run(LostAndFoundApplication.class, args);
    }

}
