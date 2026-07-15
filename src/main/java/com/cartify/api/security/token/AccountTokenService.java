package com.cartify.api.security.token;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.AccountTokenEntity;
import com.cartify.api.identity.domain.AccountTokenType;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.identity.repository.AccountTokenRepository;
import com.cartify.api.security.config.SecurityProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountTokenService {

    private final AccountTokenRepository repository;
    private final OpaqueTokenGenerator generator;
    private final TokenHashing hashing;
    private final SecurityProperties properties;
    private final Clock clock;

    public AccountTokenService(
            AccountTokenRepository repository,
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
    public String issue(UserEntity user, AccountTokenType type) {
        repository.deleteByUserIdAndType(user.getId(), type);
        String rawToken = generator.generate();
        LocalDateTime expiresAt = now().plus(properties.accountTokenTtl());
        repository.save(new AccountTokenEntity(user, type, hashing.sha256(rawToken), expiresAt));
        return rawToken;
    }

    @Transactional
    public UserEntity consume(String rawToken, AccountTokenType type) {
        AccountTokenEntity token = repository.findByHashAndTypeForUpdate(hashing.sha256(rawToken), type)
                .orElseThrow(this::invalidToken);
        if (!token.isUsable(now())) {
            throw invalidToken();
        }
        token.consume();
        return token.getUser();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private ApiException invalidToken() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ACCOUNT_TOKEN",
                "The account token is invalid, expired, or already used");
    }
}
