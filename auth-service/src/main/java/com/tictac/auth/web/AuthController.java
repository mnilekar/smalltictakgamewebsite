package com.tictac.auth.web;

import com.tictac.auth.dto.LoginRequest;
import com.tictac.auth.dto.LoginResponse;
import com.tictac.auth.dto.RegistrationRequest;
import com.tictac.auth.dto.RegistrationResponse;
import com.tictac.auth.dto.UsernameSuggestionResponse;
import com.tictac.auth.service.LoginService;
import com.tictac.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(RegistrationService registrationService, LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest req) {
        return registrationService.register(req);
    }

    @GetMapping("/suggest")
    public UsernameSuggestionResponse suggest(@RequestParam("first") String first,
                                              @RequestParam("last") String last) {
        return new UsernameSuggestionResponse(registrationService.suggestUsernames(first, last));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return loginService.login(req);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return Map.of("username", user.getUsername());
    }
}
