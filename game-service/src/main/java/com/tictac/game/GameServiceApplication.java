package com.tictac.game;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class GameServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}


# game-service/src/main/resources/application.yml
spring:
application:
name: game-service
server:
port: 8083
management:
endpoints:
web:
exposure:
include: "health,info"