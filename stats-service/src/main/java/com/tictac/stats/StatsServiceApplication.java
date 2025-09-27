package com.tictac.stats;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class StatsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsServiceApplication.class, args);
    }
}


# stats-service/src/main/resources/application.yml
spring:
application:
name: stats-service
server:
port: 8085
management:
endpoints:
web:
exposure:
include: "health,info"