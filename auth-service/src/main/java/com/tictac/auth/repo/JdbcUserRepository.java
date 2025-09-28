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
                .usingGeneratedKeyColumns("USER_ID");
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE USERNAME = :u";
        Integer c = jdbc.queryForObject(sql, Map.of("u", username), Integer.class);
        return c != null && c > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM USERS WHERE EMAIL = :e";
        Integer c = jdbc.queryForObject(sql, Map.of("e", email), Integer.class);
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

        Number key = insertUser.usingColumns(
                "USERNAME", "EMAIL", "MOBILE", "NATIONALITY", "FIRST_NAME", "LAST_NAME", "BIRTH_DATE"
        ).executeAndReturnKey(params);

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
}