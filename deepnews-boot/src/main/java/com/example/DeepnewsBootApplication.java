package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.mapper")
@EnableScheduling
@EnableConfigurationProperties
public class DeepnewsBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepnewsBootApplication.class, args);
    }
}
