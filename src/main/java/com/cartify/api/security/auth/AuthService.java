package com.cartify.api.security.auth;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.AccountTokenType;
import com.cartify.api.identity.domain.RoleEntity;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.identity.domain.UserStatus;
import com.cartify.api.identity.repository.RoleRepository;
import com.cartify.api.identity.repository.UserRepository;
import com.cartify.api.security.AuthenticatedUser;
import com.cartify.api.security.auth.dto.ActionResponse;
import com.cartify.api.security.auth.dto.AuthTokensResponse;
import com.cartify.api.security.auth.dto.ForgotPasswordRequest;
import com.cartify.api.security.auth.dto.LoginRequest;
import com.cartify.api.security.auth.dto.RegisterRequest;
import com.cartify.api.security.auth.dto.RegistrationResponse;
import com.cartify.api.security.auth.dto.ResetPasswordRequest;
import com.cartify.api.security.auth.dto.UserSummary;
import com.cartify.api.security.config.SecurityProperties;
import com.cartify.api.security.notification.AuthNotificationService;
import com.cartify.api.security.token.AccountTokenService;
import com.cartify.api.security.token.JwtService;
import com.cartify.api.security.token.RefreshTokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AccountTokenService accountTokens;
    private final RefreshTokenService refreshTokens;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final AuthNotificationService notifications;
    private final SecurityProperties properties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            AccountTokenService accountTokens,
            RefreshTokenService refreshTokens,
            JwtService jwtService,
            LoginAttemptService loginAttempts,
            AuthNotificationService notifications,
            SecurityProperties properties,
            Clock clock) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.accountTokens = accountTokens;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("cartify-dummy-password");
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_REGISTERED", "Phone number is already registered");
        }
        RoleEntity customerRole = roles.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing"));
        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                phone);
        user.addRole(customerRole);
        users.save(user);

        String verificationToken = accountTokens.issue(user, AccountTokenType.EMAIL_VERIFICATION);
        notifications.sendEmailVerification(email, verificationToken);
        return new RegistrationResponse(
                user.getId(),
                email,
                "Registration succeeded; verify the email address before logging in",
                properties.exposeDevelopmentTokens() ? verificationToken : null);
    }

    public AuthTokensResponse login(LoginRequest request, RequestMetadata metadata) {
        String email = normalizeEmail(request.email());
        loginAttempts.assertAllowed(email, metadata.ipAddress());
        UserEntity user = users.findForAuthenticationByEmail(email).orElse(null);
        String storedHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        if (!passwordEncoder.matches(request.password(), storedHash) || user == null) {
            loginAttempts.recordFailure(email, metadata.ipAddress());
            throw invalidCredentials();
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Email verification is required");
        }
        assertActive(user);
        loginAttempts.recordSuccess(email, metadata.ipAddress());
        return issueSession(user, metadata);
    }

    public AuthTokensResponse refresh(String rawRefreshToken, RequestMetadata metadata) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokens.rotate(
                rawRefreshToken, metadata.ipAddress(), metadata.userAgent());
        UserEntity user = users.findForAuthenticationById(rotated.userId())
                .orElseThrow(this::invalidCredentials);
        if (!user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            refreshTokens.revoke(rotated.value());
            throw invalidCredentials();
        }
        JwtService.IssuedAccessToken access = jwtService.issue(user);
        return response(user, access, rotated.value(), rotated.expiresAt());
    }

    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    @Transactional
    public ActionResponse verifyEmail(String rawToken) {
        UserEntity user = accountTokens.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        user.verifyEmail();
        return ActionResponse.completed("Email verification completed");
    }

    @Transactional
    public ActionResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        String rawToken = users.findForAuthenticationByEmail(email)
                .filter(UserEntity::isEmailVerified)
                .map(user -> {
                    String token = accountTokens.issue(user, AccountTokenType.PASSWORD_RESET);
                    notifications.sendPasswordReset(email, token);
                    return token;
                })
                .orElse(null);
        return new ActionResponse(
                "If the account exists, password reset instructions have been sent",
                properties.exposeDevelopmentTokens() ? rawToken : null);
    }

    @Transactional
    public ActionResponse resetPassword(ResetPasswordRequest request) {
        UserEntity user = accountTokens.consume(request.token(), AccountTokenType.PASSWORD_RESET);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(user.getId());
        return ActionResponse.completed("Password reset completed; sign in again on all devices");
    }

    private AuthTokensResponse issueSession(UserEntity user, RequestMetadata metadata) {
        JwtService.IssuedAccessToken access = jwtService.issue(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokens.issue(
                user, metadata.ipAddress(), metadata.userAgent());
        return response(user, access, refresh.value(), refresh.expiresAt());
    }

    private AuthTokensResponse response(
            UserEntity user,
            JwtService.IssuedAccessToken access,
            String refreshToken,
            Instant refreshExpiresAt) {
        return new AuthTokensResponse(
                "Bearer",
                access.value(),
                secondsUntil(access.expiresAt()),
                refreshToken,
                secondsUntil(refreshExpiresAt),
                UserSummary.from(AuthenticatedUser.from(user)));
    }

    private long secondsUntil(Instant expiry) {
        return Math.max(0, Duration.between(clock.instant(), expiry).toSeconds());
    }

    private void assertActive(UserEntity user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Account is not active");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is invalid");
    }
}
