CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by_ip VARCHAR(64),
    user_agent VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);

CREATE TABLE account_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_type VARCHAR(32) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_account_tokens_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_account_tokens_user_type ON account_tokens (user_id, token_type);

CREATE TABLE login_attempts (
    attempt_key VARCHAR(64) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    window_started_at TIMESTAMP(6) NOT NULL,
    blocked_until TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (attempt_key)
);

INSERT INTO roles (name, description, system_role) VALUES
    ('CUSTOMER', 'Standard customer account', TRUE),
    ('ADMIN', 'Full platform administration', TRUE),
    ('CATALOG_MANAGER', 'Catalogue management', TRUE),
    ('ORDER_MANAGER', 'Order operations management', TRUE),
    ('SUPPORT', 'Customer support operations', TRUE);

INSERT INTO permissions (code, description) VALUES
    ('CATALOG_READ', 'Read the public catalogue'),
    ('CART_MANAGE', 'Manage the authenticated customer cart'),
    ('ORDER_CREATE', 'Create customer orders'),
    ('ORDER_READ_OWN', 'Read the authenticated customer orders'),
    ('ADMIN_MANAGE', 'Manage platform administration resources'),
    ('CATALOG_MANAGE', 'Create and update catalogue resources'),
    ('ORDER_MANAGE', 'Manage all customer orders'),
    ('ORDER_READ_ALL', 'Read all customer orders'),
    ('SUPPORT_MANAGE', 'Access support management resources');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON
    (r.name = 'CUSTOMER' AND p.code IN ('CATALOG_READ', 'CART_MANAGE', 'ORDER_CREATE', 'ORDER_READ_OWN'))
    OR (r.name = 'ADMIN')
    OR (r.name = 'CATALOG_MANAGER' AND p.code IN ('CATALOG_READ', 'CATALOG_MANAGE'))
    OR (r.name = 'ORDER_MANAGER' AND p.code IN ('CATALOG_READ', 'ORDER_MANAGE', 'ORDER_READ_ALL'))
    OR (r.name = 'SUPPORT' AND p.code IN ('CATALOG_READ', 'ORDER_READ_ALL', 'SUPPORT_MANAGE'));
