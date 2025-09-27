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