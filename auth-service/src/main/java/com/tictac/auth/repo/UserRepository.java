package com.tictac.auth.repo;
import java.util.Optional;
import java.time.LocalDate;

public interface UserRepository {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCaseExcludingId(String username, long userId);
    boolean existsByEmailIgnoreCaseExcludingId(String email, long userId);

    /** Inserts into USERS and returns generated user_id. */
    long insertUser(String username, String email, String mobile, String nationality,
                    String firstName, String lastName, LocalDate birthDate);

    /** Inserts password hash & salt for the given user_id. */
    void insertCredentials(long userId, String passwordHash, String salt);
    Optional<UserWithHash> findByUsername(String username);
    Optional<UserProfile> findProfileById(long userId);
    Optional<UserProfileDetails> findProfileDetailsByUsername(String username);
    void updateProfile(long userId, java.util.Map<String, Object> updates);
    void updateCredentials(long userId, String passwordHash, String salt);
}
