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
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The local profile connects to the `cartify` database on port `3307` using the defaults in `application-local.yml`, and Redis is exposed on port `6379`. Override the database connection with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

For production, activate the `prod` profile and provide all three database variables; no production credentials are committed:

```shell
java -jar target/cartify-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Create each schema change as a new migration under `src/main/resources/db/migration`, for example `V2__create_products.sql`. Never edit a migration after it has been applied.
