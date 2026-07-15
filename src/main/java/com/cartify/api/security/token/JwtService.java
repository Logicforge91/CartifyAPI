package com.cartify.api.security.token;

import com.cartify.api.common.exception.ApiException;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.security.config.SecurityProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HEADER = URL_ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecretKeySpec signingKey;

    public JwtService(ObjectMapper objectMapper, SecurityProperties properties, Clock clock) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(properties.jwt().secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("app.security.jwt.secret must be Base64 encoded", exception);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must decode to at least 32 bytes");
        }
        this.signingKey = new SecretKeySpec(secret, "HmacSHA256");
    }

    public IssuedAccessToken issue(UserEntity user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", properties.jwt().issuer());
        claims.put("sub", user.getId().toString());
        claims.put("type", "access");
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        try {
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String content = HEADER + "." + payload;
            String signature = URL_ENCODER.encodeToString(sign(content));
            return new IssuedAccessToken(content + "." + signature, expiresAt);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize access token", exception);
        }
    }

    public AccessTokenClaims parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw invalidToken();
            }
            JsonNode header = objectMapper.readTree(URL_DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").stringValue())) {
                throw invalidToken();
            }
            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] actual = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw invalidToken();
            }

            JsonNode claims = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            if (!properties.jwt().issuer().equals(claims.path("iss").stringValue())
                    || !"access".equals(claims.path("type").stringValue())) {
                throw invalidToken();
            }
            Instant expiresAt = Instant.ofEpochSecond(claims.path("exp").asLong(0));
            if (!expiresAt.isAfter(clock.instant())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Access token has expired");
            }
            Long userId = Long.valueOf(claims.path("sub").stringValue());
            return new AccessTokenClaims(userId);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access token is invalid");
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {
    }

    public record AccessTokenClaims(Long userId) {
    }
}
