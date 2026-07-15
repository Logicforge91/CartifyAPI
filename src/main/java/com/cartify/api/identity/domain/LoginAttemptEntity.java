package com.cartify.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
public class LoginAttemptEntity {

    @Id
    @Column(name = "attempt_key", length = 64)
    private String attemptKey;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LoginAttemptEntity() {
    }

    public LoginAttemptEntity(String attemptKey, LocalDateTime now) {
        this.attemptKey = attemptKey;
        this.windowStartedAt = now;
        this.updatedAt = now;
    }

    public boolean isBlocked(LocalDateTime now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }

    public void recordFailure(LocalDateTime now, int maxAttempts, java.time.Duration window, java.time.Duration block) {
        if (windowStartedAt.plus(window).isBefore(now)) {
            failedAttempts = 0;
            windowStartedAt = now;
            blockedUntil = null;
        }
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            blockedUntil = now.plus(block);
        }
        updatedAt = now;
    }
}
