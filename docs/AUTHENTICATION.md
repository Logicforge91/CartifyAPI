# Authentication and authorization

## Endpoints

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a customer and issue email verification |
| POST | `/api/v1/auth/verify-email` | Public | Consume a one-time verification token |
| POST | `/api/v1/auth/login` | Public | Authenticate and issue access/refresh tokens |
| POST | `/api/v1/auth/refresh` | Public | Rotate a refresh token and issue a new access token |
| POST | `/api/v1/auth/logout` | Public with refresh token | Revoke a refresh token |
| POST | `/api/v1/auth/forgot-password` | Public | Request password reset without revealing account existence |
| POST | `/api/v1/auth/reset-password` | Public | Consume a reset token, update the password, and revoke sessions |
| GET | `/api/v1/auth/me` | Authenticated | Return the current identity, roles, and permissions |

All responses use the standard `ApiResponse` envelope. Access tokens are sent as `Authorization: Bearer <token>`.

## Token policy

- Access tokens are HMAC-SHA256 signed JWTs and expire after 15 minutes.
- Refresh tokens are cryptographically random opaque values and expire after 30 days.
- Only SHA-256 refresh/account-token hashes are stored in the database.
- Every refresh rotates the token. Reuse of an already rotated token revokes the complete token family.
- Logout revokes the submitted refresh token. Existing access tokens remain valid only until their short expiry.
- Email-verification and password-reset tokens are single-use and expire after 30 minutes.
- Password reset revokes every active refresh token for the user.

Durations and login-throttling limits can be overridden through the `app.security` configuration properties.

## Roles and route rules

- `CUSTOMER`
- `ADMIN`
- `CATALOG_MANAGER`
- `ORDER_MANAGER`
- `SUPPORT`

GET requests under `/api/v1/catalog/**` are public. `/api/v1/cart/**` and `/api/v1/orders/**` require authentication. `/api/v1/admin/**` requires `ADMIN`. Catalogue, order, and support management namespaces require their corresponding permissions or `ADMIN_MANAGE`.

## Local verification/reset tokens

The `local`, `dev`, and `test` profiles expose one-time tokens in responses and log development links. This is disabled in `prod`, where tokens are delivered using Spring Mail and SMTP.

## Initial administrator

Set both variables before the first startup:

```properties
APP_INITIAL_ADMIN_EMAIL=admin@example.com
APP_INITIAL_ADMIN_PASSWORD=a-long-unique-password
```

The bootstrap is idempotent and will not overwrite an existing account. The initial password must contain at least 12 characters.

## Production configuration

Provide a Base64-encoded random secret that decodes to at least 32 bytes:

```properties
JWT_SECRET=<base64-secret>
FRONTEND_BASE_URL=https://app.example.com
APP_MAIL_FROM=no-reply@example.com
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<username>
SPRING_MAIL_PASSWORD=<password>
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

Never reuse local secrets or commit production credentials.
