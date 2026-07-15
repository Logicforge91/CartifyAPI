package com.cartify.api.security.notification;

public interface AuthNotificationService {

    void sendEmailVerification(String email, String rawToken);

    void sendPasswordReset(String email, String rawToken);
}
