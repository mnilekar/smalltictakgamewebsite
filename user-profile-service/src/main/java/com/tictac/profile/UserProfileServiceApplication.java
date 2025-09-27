package com.tictac.profile;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class UserProfileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserProfileServiceApplication.class, args);
    }
}


# user-profile-service/src/main/resources/application.yml
spring:
application:
name: user-profile-service
server:
port: 8082
management:
endpoints:
web:
exposure:
include: "health,info"