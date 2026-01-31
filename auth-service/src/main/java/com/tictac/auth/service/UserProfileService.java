package com.tictac.auth.service;

import com.tictac.auth.dto.ProfileUpdateRequest;
import com.tictac.auth.repo.UserProfile;
import com.tictac.auth.repo.UserProfileDetails;
import com.tictac.auth.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class UserProfileService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserProfileService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public UserProfile findProfileById(long userId) {
        return repo.findProfileById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserProfileDetails findProfileDetailsByUsername(String username) {
        return repo.findProfileDetailsByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserProfileDetails updateProfile(String username, ProfileUpdateRequest req) {
        UserProfileDetails existing = findProfileDetailsByUsername(username);
        Map<String, Object> updates = new HashMap<>();

        String firstName = normalize(req.getFirstName());
        if (firstName != null && !firstName.equals(existing.firstName())) {
            updates.put("FIRST_NAME", firstName);
        }

        String lastName = normalize(req.getLastName());
        if (lastName != null && !lastName.equals(existing.lastName())) {
            updates.put("LAST_NAME", lastName);
        }

        if (req.getBirthDate() != null && !req.getBirthDate().equals(existing.birthDate())) {
            updates.put("BIRTH_DATE", java.sql.Date.valueOf(req.getBirthDate()));
        }

        String nationality = normalize(req.getNationality());
        if (nationality != null && !nationality.equals(existing.nationality())) {
            updates.put("NATIONALITY", nationality);
        }

        String email = normalize(req.getEmail());
        if (email != null && !email.equalsIgnoreCase(existing.email())) {
            if (repo.existsByEmailIgnoreCaseExcludingId(email, existing.userId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            updates.put("EMAIL", email);
        }

        String mobile = normalize(req.getMobile());
        if (mobile != null && !mobile.equals(existing.mobile())) {
            updates.put("MOBILE", mobile);
        }

        String newUsername = normalize(req.getUsername());
        if (newUsername != null && !newUsername.equalsIgnoreCase(existing.username())) {
            if (repo.existsByUsernameIgnoreCaseExcludingId(newUsername, existing.userId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
            }
            updates.put("USERNAME", newUsername);
        }

        if (!updates.isEmpty()) {
            repo.updateProfile(existing.userId(), updates);
        }

        String password = req.getPassword();
        if (password != null && !password.isBlank()) {
            if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8,}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Password must be ≥8 chars and include upper, lower, and special character");
            }
            String salt = HexFormat.of().formatHex(randomBytes(16));
            String hash = encoder.encode(password);
            repo.updateCredentials(existing.userId(), hash, salt);
        }

        String lookup = newUsername != null ? newUsername : existing.username();
        return findProfileDetailsByUsername(lookup);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields cannot be blank");
        }
        return trimmed;
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }
}
