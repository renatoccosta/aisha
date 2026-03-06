# Development Guide

This document centralizes technical development details for AI$HA.

## Security Architecture

Security details moved to a dedicated document:

- [docs/security-architecture.md](security-architecture.md)

Current baseline summary:

- Spring Security with authenticated-by-default routes
- Form login + secure server-side sessions (`JSESSIONID`)
- OAuth2/OIDC login integration (current provider registration: Google)
- Global CSRF protection (including HTMX forms)
- Session hardening (fixation protection, idle timeout, absolute timeout, and concurrent session limit)


## Logging Architecture

- Logging API for application code: SLF4J (`org.slf4j.Logger`).
- Logging implementation: Logback via Spring Boot defaults (configured through `application.yaml` logging properties).
- Configuration source: Spring-managed logging properties in `application.yaml` (no custom Logback XML by default).
- Output target: console only (no file appender configured).
- Correlation ID:
  - Request filter accepts `X-Correlation-Id` when provided, or generates a UUID.
  - Correlation ID is stored in MDC (`correlationId`) and returned in `X-Correlation-Id` response header.
  - Log pattern includes the correlation ID for request traceability.
- Error handling and observability:
  - All unexpected errors returned to users are logged with technical details and correlation ID.
  - Controller-level handled errors (e.g., 400/404 business flows) are also logged with correlation ID and request metadata.

## Default Local User (Development Bootstrap)

At startup, the application seeds a local user if missing:

- username: `admin`
- password: `admin`

Configurable properties:

- `aisha.security.seed.username`
- `aisha.security.seed.password`

Important: change default credentials in non-development environments.

## Technology Stack

- Java 25 (LTS)
- Spring Boot 4.0.2
- Spring MVC + Thymeleaf
- Spring Data JPA
- Flyway
- Bean Validation
- HSQLDB (development)
- PostgreSQL (production/persistent environments)
- Maven Wrapper (`mvnw`)

## Prerequisites

- JDK 25 installed and configured in `PATH`
- Optional: PostgreSQL 14+ to run with the `postgres` profile

## Database and Initialization

- Database schema is versioned and applied automatically at startup via Flyway migrations (`src/main/resources/db/migration`).
- Hibernate runs in validation mode (`ddl-auto: validate`) to ensure mapping/schema consistency.
- Initial seed data is loaded from `src/main/resources/data.sql` in embedded mode.

## Tests

Run the test suite with:

```bash
./mvnw test
```

## Project Structure

Main organization:

- `src/main/java/dev/ccosta/aisha/domain` - domain entities and contracts
- `src/main/java/dev/ccosta/aisha/application` - application rules and use cases
- `src/main/java/dev/ccosta/aisha/infrastructure` - persistence and technical integrations
- `src/main/java/dev/ccosta/aisha/web` - MVC controllers and forms
- `src/main/resources/templates` - Thymeleaf pages and fragments
- `src/main/resources/static` - CSS and static assets
- `src/test` - unit and integration tests

## Front-End Architecture

For the full front-end architecture documentation, see:

- [docs/frontend-architecture.md](frontend-architecture.md)

## Main Web Endpoints

- `/` redirects to `/dashboard`
- `/login`
- `/logout` (POST)
- `/dashboard`
- `/entries`
- `/entries/import`
- `/entries/category-model/retrain` (POST, manual background retraining command)
- `/accounts`
- `/categories`
