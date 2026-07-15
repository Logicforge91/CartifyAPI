package com.cartify.api.security.auth;

import com.cartify.api.common.api.ApiResponse;
import com.cartify.api.security.AuthenticatedUser;
import com.cartify.api.security.auth.dto.ActionResponse;
import com.cartify.api.security.auth.dto.AuthTokensResponse;
import com.cartify.api.security.auth.dto.ForgotPasswordRequest;
import com.cartify.api.security.auth.dto.LoginRequest;
import com.cartify.api.security.auth.dto.RegisterRequest;
import com.cartify.api.security.auth.dto.RegistrationResponse;
import com.cartify.api.security.auth.dto.ResetPasswordRequest;
import com.cartify.api.security.auth.dto.TokenRequest;
import com.cartify.api.security.auth.dto.UserSummary;
import com.cartify.api.security.auth.dto.VerifyEmailRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered", authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokensResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success("Login succeeded", authService.login(request, metadata(servletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokensResponse> refresh(
            @Valid @RequestBody TokenRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(
                "Tokens refreshed",
                authService.refresh(request.refreshToken(), metadata(servletRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<ActionResponse> logout(@Valid @RequestBody TokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(ActionResponse.completed("Logout completed"));
    }

    @PostMapping("/verify-email")
    public ApiResponse<ActionResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(authService.verifyEmail(request.token()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ActionResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success(authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ApiResponse<ActionResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(authService.resetPassword(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(UserSummary.from(user));
    }

    private RequestMetadata metadata(HttpServletRequest request) {
        return new RequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
