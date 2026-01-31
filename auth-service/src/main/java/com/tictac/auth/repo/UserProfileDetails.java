package com.tictac.auth.repo;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfileDetails(
        long userId,
        String username,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String nationality,
        String email,
        String mobile,
        Instant createdAt,
        Instant updatedAt
) {}
