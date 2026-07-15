# CartifyAPI

CartifyAPI is a RESTful backend service for e-commerce platforms, built with Java and Spring Boot. It handles core commerce operations such as product catalog, cart, checkout, and order management through clean, versioned API endpoints.

## Local infrastructure

MySQL is the primary database. Flyway is the only mechanism that creates or changes application tables; Hibernate only validates the mapped schema. Redis is provisioned for future caching, carts, OTPs, and rate limiting, but is not connected to the application yet.

Start MySQL and Redis:

```shell
docker compose up -d
```

Run the application with the default `local` profile:

```shell
./mvnw spring-boot:run
```

The `dev` profile includes the same local infrastructure configuration and can be started on Windows with:

```shell
mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

The local profile connects to the `cartify` database on port `3307` using the defaults in `application-local.yml`, and Redis is exposed on port `6379`. Override the database connection with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Copy `.env.example` to `.env` when local environment overrides are needed. Never commit `.env` or production secrets.

For production, activate the `prod` profile and provide all three database variables; no production credentials are committed:

```shell
java -jar target/cartify-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Create each schema change as a new migration under `src/main/resources/db/migration`, for example `V2__create_products.sql`. Never edit a migration after it has been applied.

## API conventions

- Successes and errors use the shared `ApiResponse` envelope.
- Validation errors return HTTP 400 with code `VALIDATION_FAILED` and field-level messages.
- Missing resources return HTTP 404 with code `RESOURCE_NOT_FOUND`.
- Data conflicts return HTTP 409 with code `DATA_CONFLICT`.
- Pagination is zero-based, defaults to 20 items, and is capped at 100 items per page. Paged results use `PageResponse`.
- The health endpoint is available at `GET /actuator/health`; only `health` and `info` actuator endpoints are exposed.

See `docs/BRANCHING.md` for the repository branching and promotion workflow.

## Authentication

Authentication uses short-lived signed access tokens and rotating opaque refresh tokens. Customer registration, email verification, login, refresh, logout, forgot/reset password, login throttling, and role-based authorization are available under `/api/v1/auth`.

See `docs/AUTHENTICATION.md` for endpoint payloads, route rules, roles, token lifetimes, administrator bootstrap, and production SMTP configuration.
