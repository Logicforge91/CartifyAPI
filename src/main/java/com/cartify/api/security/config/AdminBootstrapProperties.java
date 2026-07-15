package com.cartify.api.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record AdminBootstrapProperties(String email, String password) {

    public boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
