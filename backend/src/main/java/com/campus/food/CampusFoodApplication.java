package com.campus.food;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.campus.food.mapper")
public class CampusFoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusFoodApplication.class, args);
    }
}