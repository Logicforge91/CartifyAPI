package com.cartify.api.security.auth.dto;

public record ActionResponse(String message, String developmentToken) {

    public static ActionResponse completed(String message) {
        return new ActionResponse(message, null);
    }
}
