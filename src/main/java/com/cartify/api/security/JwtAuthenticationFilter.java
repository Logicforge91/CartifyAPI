package com.cartify.api.security;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.identity.domain.UserStatus;
import com.cartify.api.identity.repository.UserRepository;
import com.cartify.api.security.token.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository users,
            SecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.users = users;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String rawToken = authorization.substring(7).trim();
            JwtService.AccessTokenClaims claims = jwtService.parse(rawToken);
            UserEntity user = users.findForAuthenticationById(claims.userId())
                    .filter(candidate -> candidate.isEmailVerified() && candidate.getStatus() == UserStatus.ACTIVE)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access token is invalid"));
            AuthenticatedUser principal = AuthenticatedUser.from(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(
                    request,
                    response,
                    exception.getStatus().value(),
                    exception.getCode(),
                    exception.getMessage());
        }
    }
}
