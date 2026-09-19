package com.trading.signal.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessMs;
        this.refreshTokenExpirationMs = refreshMs;
    }

    public String generateAccessToken(Long userId, String email) {
        return buildToken(email, userId, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(Long userId, String email) {
        return buildToken(email, userId, refreshTokenExpirationMs, "refresh");
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    private String buildToken(String subject, Long userId, long expirationMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
