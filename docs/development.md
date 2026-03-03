# Development Guide

This document centralizes technical development details for AI$HA.

## Security Architecture (Current Baseline)

The current implementation uses local authentication with secure server-side sessions as a baseline, while keeping the codebase ready for future OAuth2/OIDC evolution.

- Security framework: Spring Security
- Authentication flow: form login (`/login`) + session cookie (`JSESSIONID`)
- User store: local database table `local_user_accounts` (JPA)
- Password storage: BCrypt hash
- Protected routes: all routes require authentication except login and static assets
- CSRF: enabled by default and enforced for state-changing requests (including HTMX form flows)
- Session fixation: mitigated via session ID regeneration after login
- Session policy:
  - idle timeout: 45 minutes
  - absolute timeout: 12 hours (custom filter)
  - concurrent sessions per user: 1 (new login invalidates previous session)
- Session cookie policy:
  - `HttpOnly=true`
  - `SameSite=Lax`
  - `Secure=true` in `prod` profile
- Security audit logs (minimum):
  - login success
  - login failure
  - logout
  - no password logging


## Logging Architecture

- Logging API for application code: SLF4J (`org.slf4j.Logger`).
- Logging implementation: Logback via Spring Boot defaults (`logback-spring.xml`).
- Configuration source: Spring-managed logging configuration (no direct Logback bootstrap code).
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

- `docs/frontend-architecture.md`

## Main Web Endpoints

- `/` redirects to `/dashboard`
- `/login`
- `/logout` (POST)
- `/dashboard`
- `/entries`
- `/accounts`
- `/categories`
