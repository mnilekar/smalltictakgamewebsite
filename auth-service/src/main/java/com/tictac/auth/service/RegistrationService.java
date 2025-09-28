package com.tictac.auth.service;

import com.tictac.auth.dto.RegistrationRequest;
import com.tictac.auth.dto.RegistrationResponse;
import com.tictac.auth.repo.UserRepository;
import com.tictac.auth.util.UsernameSuggester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HexFormat;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest r) {
        // Extra server-side password guard (in addition to @Pattern)
        if (!r.getPassword().matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8,}$")) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Password must be ≥8 chars and include upper, lower, and special character");
        }

        if (repo.existsByUsername(r.getUsername())) {
            throw new ResponseStatusException(CONFLICT, "Username already taken");
        }
        if (repo.existsByEmail(r.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "Email already in use");
        }

        long userId = repo.insertUser(
                r.getUsername(), r.getEmail(), r.getMobile(), r.getNationality(),
                r.getFirstName(), r.getLastName(), r.getBirthDate()
        );

        // BCrypt already salts internally; we also store a random salt for auditing (schema requires not null)
        String salt = HexFormat.of().formatHex(randomBytes(16));
        String hash = encoder.encode(r.getPassword());

        repo.insertCredentials(userId, hash, salt);

        log.info("REGISTER user={} id={}", r.getUsername(), userId);
        return new RegistrationResponse(userId, r.getUsername());
    }

    public java.util.List<String> suggestUsernames(String first, String last) {
        return UsernameSuggester.suggest(first, last);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }
}