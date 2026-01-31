package com.tictac.auth.web;

import com.tictac.auth.dto.ProfileUpdateRequest;
import com.tictac.auth.repo.UserProfileDetails;
import com.tictac.auth.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public UserProfileDetails me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return userProfileService.findProfileDetailsByUsername(user.getUsername());
    }

    @PutMapping("/me")
    public UserProfileDetails update(@AuthenticationPrincipal User user,
                                     @Valid @RequestBody ProfileUpdateRequest req) {
        if (user == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return userProfileService.updateProfile(user.getUsername(), req);
    }
}
