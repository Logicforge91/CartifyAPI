package com.cartify.api.security.auth;

import com.cartify.api.identity.domain.RoleEntity;
import com.cartify.api.identity.domain.UserEntity;
import com.cartify.api.identity.repository.RoleRepository;
import com.cartify.api.identity.repository.UserRepository;
import com.cartify.api.security.config.AdminBootstrapProperties;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminBootstrapProperties properties;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            AdminBootstrapProperties properties,
            UserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!properties.configured()) {
            return;
        }
        if (properties.password().length() < 12) {
            throw new IllegalStateException("APP_INITIAL_ADMIN_PASSWORD must contain at least 12 characters");
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        RoleEntity adminRole = roles.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role is missing"));
        UserEntity admin = new UserEntity(email, passwordEncoder.encode(properties.password()), "Platform", "Admin", null);
        admin.addRole(adminRole);
        admin.activateVerifiedAdmin();
        users.save(admin);
        log.info("Initial administrator account created for {}", email);
    }
}
