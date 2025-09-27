package com.tictac.chat;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}


# chat-service/src/main/resources/application.yml
spring:
application:
name: chat-service
server:
port: 8086
management:
endpoints:
web:
exposure:
include: "health,info"