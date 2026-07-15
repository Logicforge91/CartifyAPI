package com.cartify.api.security.token;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.RefreshTokenEntity;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.identity.repository.RefreshTokenRepository;
import com.cartify.api.security.config.SecurityProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final OpaqueTokenGenerator generator;
    private final TokenHashing hashing;
    private final SecurityProperties properties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            OpaqueTokenGenerator generator,
            TokenHashing hashing,
            SecurityProperties properties,
            Clock clock) {
        this.repository = repository;
        this.generator = generator;
        this.hashing = hashing;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(UserEntity user, String ipAddress, String userAgent) {
        return issue(user, UUID.randomUUID().toString(), ipAddress, userAgent);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public RotatedRefreshToken rotate(String rawToken, String ipAddress, String userAgent) {
        RefreshTokenEntity current = repository.findByHashForUpdate(hashing.sha256(rawToken))
                .orElseThrow(this::invalidToken);

        if (current.isRevoked()) {
            if (current.getReplacedByTokenHash() != null) {
                repository.findByFamilyIdAndRevokedAtIsNull(current.getFamilyId())
                        .forEach(token -> token.revoke(null));
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "REFRESH_TOKEN_REUSE",
                        "Refresh token reuse was detected; the token family has been revoked");
            }
            throw invalidToken();
        }
        if (current.isExpired(now())) {
            current.revoke(null);
            throw invalidToken();
        }

        IssuedRefreshToken replacement = issue(
                current.getUser(), current.getFamilyId(), ipAddress, userAgent);
        current.revoke(hashing.sha256(replacement.value()));
        return new RotatedRefreshToken(current.getUser().getId(), replacement.value(), replacement.expiresAt());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByHashForUpdate(hashing.sha256(rawToken))
                .ifPresent(token -> token.revoke(null));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.revoke(null));
    }

    private IssuedRefreshToken issue(
            UserEntity user,
            String familyId,
            String ipAddress,
            String userAgent) {
        String rawToken = generator.generate();
        Instant expiresAt = clock.instant().plus(properties.refreshTokenTtl());
        repository.save(new RefreshTokenEntity(
                user,
                hashing.sha256(rawToken),
                familyId,
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC),
                limit(ipAddress, 64),
                limit(userAgent, 255)));
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String value, Instant expiresAt) {
    }

    public record RotatedRefreshToken(Long userId, String value, Instant expiresAt) {
    }
}
