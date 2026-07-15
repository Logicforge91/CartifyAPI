package com.cartify.api.security.auth.dto;

public record RegistrationResponse(
        Long userId,
        String email,
        String message,
        String developmentVerificationToken) {
}
