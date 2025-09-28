package com.tictac.auth.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class JwtUtil {
    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;


    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.issuer}") String issuer,
                   @Value("${app.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }


    public String generate(long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claims(Map.of("uid", userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }


    public io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}