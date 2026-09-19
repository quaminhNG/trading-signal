package com.trading.signal.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100, message = "Full name max 100 characters")
        String fullName
) {}
