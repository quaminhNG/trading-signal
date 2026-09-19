package com.trading.signal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        // 32+ chars secret for HS256
        provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-characters-long",
                900000,   // 15 min access
                604800000 // 7 days refresh
        );
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = provider.generateAccessToken(1L, "test@example.com");
        assertTrue(provider.validateToken(token));
        assertEquals("test@example.com", provider.getEmailFromToken(token));
        assertEquals("access", provider.getTokenType(token));
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = provider.generateRefreshToken(1L, "test@example.com");
        assertTrue(provider.validateToken(token));
        assertEquals("refresh", provider.getTokenType(token));
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(provider.validateToken("garbage.token.here"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken(null));
    }
}
