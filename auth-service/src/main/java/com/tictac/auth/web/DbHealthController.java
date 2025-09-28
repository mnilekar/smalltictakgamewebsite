package com.tictac.auth.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbHealthController {

    private final JdbcTemplate jdbc;

    public DbHealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/auth/db/ping")
    public String ping() {
        Integer one = jdbc.queryForObject("SELECT 1 FROM dual", Integer.class);
        return "db-ok:" + one; // expect db-ok:1
    }
}
