package com.tictac.ui;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigController {

    @GetMapping(value = "/config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> config() {
        String authBase = readEnv("AUTH_BASE", "http://localhost:8081");
        String gameBase = readEnv("GAME_BASE", "http://localhost:8091");
        String wsBase = readEnv("WS_BASE", "ws://localhost:8091/ws");
        return Map.of(
                "authBase", authBase,
                "gameBase", gameBase,
                "wsBase", wsBase
        );
    }

    private String readEnv(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
