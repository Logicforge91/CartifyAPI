package com.cartify.api.security.notification;

import com.cartify.api.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Profile("!prod")
public class DevelopmentAuthNotificationService implements AuthNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentAuthNotificationService.class);

    private final SecurityProperties properties;

    public DevelopmentAuthNotificationService(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        log.info("Development email verification for {}: {}", email, link("/verify-email", rawToken));
    }

    @Override
    public void sendPasswordReset(String email, String rawToken) {
        log.info("Development password reset for {}: {}", email, link("/reset-password", rawToken));
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(properties.frontendBaseUrl())
                .path(path)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
