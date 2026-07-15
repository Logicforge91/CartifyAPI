package com.cartify.api.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @NotNull Duration refreshTokenTtl,
        @NotNull Duration accountTokenTtl,
        @Valid @NotNull Login login,
        boolean exposeDevelopmentTokens,
        @NotBlank String frontendBaseUrl,
        @NotBlank String mailFrom) {

    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String secret,
            @NotNull Duration accessTokenTtl) {
    }

    public record Login(
            @Min(1) int maxAttempts,
            @NotNull Duration window,
            @NotNull Duration blockDuration) {
    }
}
