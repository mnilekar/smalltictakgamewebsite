package com.tictac.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey key;          // HMAC key must be javax.crypto.SecretKey
    private final String issuer;
    private final long ttlSeconds;

    public JwtUtil(
            @Value("${app.jwt.secret:change-me-change-me-change-me-change-me-change-me-0123456789}") String secret,
            @Value("${app.jwt.issuer:tictac-auth}") String issuer,
            @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds
    ) {
        // secret should be at least 256 bits (32+ bytes) for HS256
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    /** Generate a token with subject=username and claim uid=userId */
    public String generate(long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claims(Map.of("uid", userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)              // HS256 by default
                .compact();
    }

    /** Parse and return JWT claims (throws if invalid/expired) */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)            // requires javax.crypto.SecretKey
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
