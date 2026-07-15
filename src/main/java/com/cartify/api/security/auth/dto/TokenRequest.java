package com.cartify.api.security.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String refreshToken) {
}
