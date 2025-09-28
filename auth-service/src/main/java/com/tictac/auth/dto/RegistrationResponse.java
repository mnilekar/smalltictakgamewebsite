package com.tictac.auth.dto;

public class RegistrationResponse {
    private Long userId;
    private String username;

    public RegistrationResponse(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
}