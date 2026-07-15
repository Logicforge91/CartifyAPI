package com.cartify.api.security.auth;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.LoginAttemptEntity;
import com.cartify.api.identity.repository.LoginAttemptRepository;
import com.cartify.api.security.config.SecurityProperties;
import com.cartify.api.security.token.TokenHashing;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository repository;
    private final TokenHashing hashing;
    private final SecurityProperties properties;
    private final Clock clock;

    public LoginAttemptService(
            LoginAttemptRepository repository,
            TokenHashing hashing,
            SecurityProperties properties,
            Clock clock) {
        this.repository = repository;
        this.hashing = hashing;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void assertAllowed(String normalizedEmail, String ipAddress) {
        LocalDateTime now = now();
        for (String key : keys(normalizedEmail, ipAddress)) {
            repository.findByKeyForUpdate(key).ifPresent(attempt -> {
                if (attempt.isBlocked(now)) {
                    throw blocked();
                }
            });
        }
    }

    @Transactional
    public void recordFailure(String normalizedEmail, String ipAddress) {
        LocalDateTime now = now();
        SecurityProperties.Login login = properties.login();
        for (String key : keys(normalizedEmail, ipAddress)) {
            LoginAttemptEntity attempt = repository.findByKeyForUpdate(key)
                    .orElseGet(() -> new LoginAttemptEntity(key, now));
            attempt.recordFailure(now, login.maxAttempts(), login.window(), login.blockDuration());
            repository.save(attempt);
        }
    }

    @Transactional
    public void recordSuccess(String normalizedEmail, String ipAddress) {
        repository.deleteAllByIdInBatch(keys(normalizedEmail, ipAddress));
    }

    private List<String> keys(String email, String ipAddress) {
        return List.of(
                hashing.sha256("EMAIL|" + email),
                hashing.sha256("IP|" + (ipAddress == null ? "unknown" : ipAddress)));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private ApiException blocked() {
        return new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "LOGIN_RATE_LIMITED",
                "Too many login attempts; try again later");
    }
}
