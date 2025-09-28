package com.tictac.auth.dto;


public class LoginResponse {
    private final String token;
    private final long userId;
    private final String username;
    public LoginResponse(String token, long userId, String username) {
        this.token = token; this.userId = userId; this.username = username;
    }
    public String getToken() { return token; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
}