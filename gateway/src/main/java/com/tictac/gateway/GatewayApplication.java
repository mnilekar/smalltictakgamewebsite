package com.tictac.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}


# gateway/src/main/resources/application.yml
spring:
application:
name: gateway
server:
port: 8080
management:
endpoints:
web:
exposure:
include: "health,info"