package com.cartify.api.security.token;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class OpaqueTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
