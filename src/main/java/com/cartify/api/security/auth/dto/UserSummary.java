package com.cartify.api.security.auth.dto;

import com.cartify.api.security.AuthenticatedUser;
import java.util.Set;

public record UserSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        Set<String> permissions) {

    public static UserSummary from(AuthenticatedUser user) {
        return new UserSummary(
                user.id(), user.email(), user.firstName(), user.lastName(), user.roles(), user.permissions());
    }
}
