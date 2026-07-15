package com.cartify.api.security.notification;

import com.cartify.api.security.config.SecurityProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Profile("prod")
public class SmtpAuthNotificationService implements AuthNotificationService {

    private final JavaMailSender mailSender;
    private final SecurityProperties properties;

    public SmtpAuthNotificationService(JavaMailSender mailSender, SecurityProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        send(email, "Verify your Cartify email", "Verify your email: " + link("/verify-email", rawToken));
    }

    @Override
    public void sendPasswordReset(String email, String rawToken) {
        send(email, "Reset your Cartify password", "Reset your password: " + link("/reset-password", rawToken));
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(properties.frontendBaseUrl())
                .path(path)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
