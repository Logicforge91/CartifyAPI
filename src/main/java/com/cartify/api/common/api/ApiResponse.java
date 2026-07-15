package com.cartify.api.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Map<String, String> errors,
        Instant timestamp,
        String path) {

    public ApiResponse {
        errors = errors == null ? Map.of() : Map.copyOf(errors);
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, Map.of(), Instant.now(), null);
    }

    public static ApiResponse<Void> failure(String code, String message, String path) {
        return failure(code, message, Map.of(), path);
    }

    public static ApiResponse<Void> failure(
            String code,
            String message,
            Map<String, String> errors,
            String path) {
        return new ApiResponse<>(false, code, message, null, errors, Instant.now(), path);
    }
}
