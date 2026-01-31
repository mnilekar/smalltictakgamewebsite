package com.tictac.auth.repo;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final SimpleJdbcInsert insertUser;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insertUser = new SimpleJdbcInsert(jdbc.getJdbcTemplate())
                .withTableName("USERS")
                .usingGeneratedKeyColumns("USER_ID")
                .usingColumns("USERNAME", "EMAIL", "MOBILE", "NATIONALITY", "FIRST_NAME", "LAST_NAME", "BIRTH_DATE");
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE LOWER(USERNAME) = LOWER(:u)";
        Integer c = jdbc.queryForObject(sql, Map.of("u", username), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE LOWER(EMAIL) = LOWER(:e)";
        Integer c = jdbc.queryForObject(sql, Map.of("e", email), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public boolean existsByUsernameIgnoreCaseExcludingId(String username, long userId) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE LOWER(USERNAME) = LOWER(:u) AND USER_ID <> :id";
        Integer c = jdbc.queryForObject(sql, Map.of("u", username, "id", userId), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public boolean existsByEmailIgnoreCaseExcludingId(String email, long userId) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE LOWER(EMAIL) = LOWER(:e) AND USER_ID <> :id";
        Integer c = jdbc.queryForObject(sql, Map.of("e", email, "id", userId), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public long insertUser(String username, String email, String mobile, String nationality,
                           String firstName, String lastName, LocalDate birthDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("USERNAME", username);
        params.put("EMAIL", email);
        params.put("MOBILE", mobile);
        params.put("NATIONALITY", nationality);
        params.put("FIRST_NAME", firstName);
        params.put("LAST_NAME", lastName);
        params.put("BIRTH_DATE", java.sql.Date.valueOf(birthDate));

        Number key = insertUser.executeAndReturnKey(params);

        return key.longValue();
    }

    @Override
    public void insertCredentials(long userId, String passwordHash, String salt) {
        String sql = """
        INSERT INTO USER_CREDENTIALS (USER_ID, PASSWORD_HASH, PASSWORD_SALT)
        VALUES (:id, :hash, :salt)
        """;
        jdbc.update(sql, Map.of(
                "id", userId,
                "hash", passwordHash,
                "salt", salt
        ));
    }
    @Override
    public java.util.Optional<UserWithHash> findByUsername(String username) {
        String sql = """
                    SELECT u.USER_ID, u.USERNAME, c.PASSWORD_HASH
                        FROM USERS u
                        JOIN USER_CREDENTIALS c ON c.USER_ID = u.USER_ID
                        WHERE u.USERNAME = :u
                    """;
        var list = jdbc.query(sql, java.util.Map.of("u", username), (rs, rowNum) ->
                new UserWithHash(rs.getLong("USER_ID"), rs.getString("USERNAME"), rs.getString("PASSWORD_HASH"))
        );
        return list.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(list.get(0));
    }

    @Override
    public java.util.Optional<UserProfile> findProfileById(long userId) {
        String sql = """
                SELECT USER_ID, USERNAME, FIRST_NAME, LAST_NAME
                FROM USERS
                WHERE USER_ID = :id
                """;
        var list = jdbc.query(sql, Map.of("id", userId), (rs, rowNum) ->
                new UserProfile(
                        rs.getLong("USER_ID"),
                        rs.getString("USERNAME"),
                        rs.getString("FIRST_NAME"),
                        rs.getString("LAST_NAME")
                )
        );
        return list.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(list.get(0));
    }

    @Override
    public java.util.Optional<UserProfileDetails> findProfileDetailsByUsername(String username) {
        String sql = """
                SELECT USER_ID, USERNAME, FIRST_NAME, LAST_NAME, BIRTH_DATE, NATIONALITY, EMAIL, MOBILE, CREATED_AT, UPDATED_AT
                FROM USERS
                WHERE LOWER(USERNAME) = LOWER(:u)
                """;
        var list = jdbc.query(sql, Map.of("u", username), (rs, rowNum) ->
                new UserProfileDetails(
                        rs.getLong("USER_ID"),
                        rs.getString("USERNAME"),
                        rs.getString("FIRST_NAME"),
                        rs.getString("LAST_NAME"),
                        rs.getDate("BIRTH_DATE") != null ? rs.getDate("BIRTH_DATE").toLocalDate() : null,
                        rs.getString("NATIONALITY"),
                        rs.getString("EMAIL"),
                        rs.getString("MOBILE"),
                        rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toInstant() : null,
                        rs.getTimestamp("UPDATED_AT") != null ? rs.getTimestamp("UPDATED_AT").toInstant() : null
                )
        );
        return list.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(list.get(0));
    }

    @Override
    public void updateProfile(long userId, Map<String, Object> updates) {
        if (updates.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("UPDATE USERS SET ");
        int i = 0;
        for (String key : updates.keySet()) {
            if (i > 0) sb.append(", ");
            sb.append(key).append(" = :").append(key);
            i++;
        }
        sb.append(", UPDATED_AT = SYSTIMESTAMP WHERE USER_ID = :id");
        Map<String, Object> params = new HashMap<>(updates);
        params.put("id", userId);
        jdbc.update(sb.toString(), params);
    }

    @Override
    public void updateCredentials(long userId, String passwordHash, String salt) {
        String sql = """
                UPDATE USER_CREDENTIALS
                SET PASSWORD_HASH = :hash,
                    PASSWORD_SALT = :salt
                WHERE USER_ID = :id
                """;
        jdbc.update(sql, Map.of(
                "id", userId,
                "hash", passwordHash,
                "salt", salt
        ));
    }
}
