package com.tictac.history;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class MatchHistoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchHistoryServiceApplication.class, args);
    }
}


# match-history-service/src/main/resources/application.yml
spring:
application:
name: match-history-service
server:
port: 8084
management:
endpoints:
web:
exposure:
include: "health,info"