package com.trading.signal.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
) {}
