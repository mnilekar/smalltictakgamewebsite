package com.tictac.auth;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}


# auth-service/src/main/java/com/tictac/auth/web/HelloController.java
package com.tictac.auth.web;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {
    @GetMapping("/api/auth/hello")
    public String hello() {
        return "auth-service up";
    }
}


# auth-service/src/main/resources/application.yml
spring:
application:
name: auth-service
server:
port: 8081
management:
endpoints:
web:
exposure:
include: "health,info"