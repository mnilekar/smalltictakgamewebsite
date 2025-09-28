package com.tictac.auth.web;

import com.tictac.auth.dto.RegistrationRequest;
import com.tictac.auth.dto.RegistrationResponse;
import com.tictac.auth.dto.UsernameSuggestionResponse;
import com.tictac.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService service;

    public AuthController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest req) {
        return service.register(req);
    }

    @GetMapping("/suggest")
    public UsernameSuggestionResponse suggest(@RequestParam("first") String first,
                                              @RequestParam("last") String last) {
        return new UsernameSuggestionResponse(service.suggestUsernames(first, last));
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody com.tictac.auth.dto.LoginRequest req) {
        return loginService.login(req);
    }


    @GetMapping("/me")
    public java.util.Map<String, Object> me(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        if (user == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return java.util.Map.of("username", user.getUsername());
    }
    private final RegistrationService service;
    private final LoginService loginService;
    public AuthController(RegistrationService service, LoginService loginService) {
        this.service = service; this.loginService = loginService;
    }

}