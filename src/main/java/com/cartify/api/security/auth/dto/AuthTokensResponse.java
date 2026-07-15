package com.cartify.api.security.auth.dto;

public record AuthTokensResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        UserSummary user) {
}
