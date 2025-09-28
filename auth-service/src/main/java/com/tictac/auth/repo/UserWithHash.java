package com.tictac.auth.repo;


public record UserWithHash(long userId, String username, String passwordHash) {}