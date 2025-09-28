package com.tictac.auth.repo;

import java.time.LocalDate;

public interface UserRepository {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Inserts into USERS and returns generated user_id. */
    long insertUser(String username, String email, String mobile, String nationality,
                    String firstName, String lastName, LocalDate birthDate);

    /** Inserts password hash & salt for the given user_id. */
    void insertCredentials(long userId, String passwordHash, String salt);
}