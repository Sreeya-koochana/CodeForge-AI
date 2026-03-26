package com.aibuilder.dto;

public record AuthResponse(
        UserResponse user,
        String token,
        String message
) {
}
