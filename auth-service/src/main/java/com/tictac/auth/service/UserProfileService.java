package com.tictac.auth.service;

import com.tictac.auth.repo.UserProfile;
import com.tictac.auth.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final UserRepository repo;

    public UserProfileService(UserRepository repo) {
        this.repo = repo;
    }

    public UserProfile findProfileById(long userId) {
        return repo.findProfileById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
