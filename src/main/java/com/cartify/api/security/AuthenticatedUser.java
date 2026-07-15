package com.cartify.api.security;

import com.cartify.api.identity.domain.RoleEntity;
import com.cartify.api.identity.domain.UserEntity;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        Set<String> permissions) {

    public static AuthenticatedUser from(UserEntity user) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        for (RoleEntity role : user.getRoles()) {
            roles.add(role.getName());
            role.getPermissions().forEach(permission -> permissions.add(permission.getCode()));
        }
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Set.copyOf(roles),
                Set.copyOf(permissions));
    }

    public Set<SimpleGrantedAuthority> authorities() {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }
}
