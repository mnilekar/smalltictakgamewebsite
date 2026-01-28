package com.tictac.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public GreetingResponse hello(@RequestParam(name = "name", defaultValue = "world") String name) {
        return new GreetingResponse(String.format("Hello, %s!", name));
    }

    public record GreetingResponse(String message) {
    }
}
