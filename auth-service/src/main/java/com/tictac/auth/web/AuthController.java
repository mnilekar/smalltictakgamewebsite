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
    public UsernameSuggestionResponse suggest(@RequestParam String first,
                                              @RequestParam String last) {
        return new UsernameSuggestionResponse(service.suggestUsernames(first, last));
    }
}