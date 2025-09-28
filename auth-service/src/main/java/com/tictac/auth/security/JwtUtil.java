package com.tictac.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final Key key;
    private final long ttlMillis;

    public JwtUtil(
            @Value("${security.jwt.secret:change-me-change-me-change-me-change-me-change-me-1234567890}") String secret,
            @Value("${security.jwt.ttlSeconds:3600}") long ttlSeconds) {
        // For HMAC keys, JJWT expects at least 256-bit secret; this default string is long enough.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000L;
    }

    public String generate(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String jwt) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwt)
                .getPayload().getSubject();
    }

    public boolean isValid(String jwt) {
        try {
            extractSubject(jwt);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
