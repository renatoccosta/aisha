# AI$HA

Personal finance manager with AI capabilities, focused on correctness, traceability, and safe architectural evolution.

## Overview

AI$HA is a web application built with Spring Boot and server-side rendering (Thymeleaf), focused on:

- financial entry management (`entries`)
- account management (`accounts`)
- category management (`categories`)
- global date-range filter for data analysis

The application starts with embedded HSQLDB by default and supports PostgreSQL through profile configuration.

## Security Architecture (Current Baseline)

The current implementation uses **local authentication with secure server-side sessions** as a baseline, while keeping the codebase ready for future OAuth2/OIDC evolution.

- Security framework: Spring Security
- Auth flow: form login (`/login`) + session cookie (`JSESSIONID`)
- User store: local database table `local_user_accounts` (JPA)
- Password storage: BCrypt hash
- Protected routes: all routes require authentication except login and static assets
- CSRF: enabled by default and enforced for state-changing requests (including HTMX form flows)
- Session fixation: mitigated via session id regeneration after login
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

### Default Local User (Development Bootstrap)

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
- Bean Validation
- HSQLDB (development)
- PostgreSQL (production/persistent environments)
- Maven Wrapper (`mvnw`)

## Prerequisites

- JDK 25 installed and configured in `PATH`
- (Optional) PostgreSQL 14+ to run with the `postgres` profile

## How to Run

### 1. Run in development mode (in-memory HSQLDB)

```bash
./mvnw spring-boot:run
```

Application available at:

- `http://localhost:8080`

### 2. Run with PostgreSQL

Set the environment variables (or use the defaults):

- `SPRING_PROFILES_ACTIVE=postgres`
- `DB_URL` (default: `jdbc:postgresql://localhost:5432/aisha`)
- `DB_USERNAME` (default: `aisha`)
- `DB_PASSWORD` (default: `aisha`)

Example:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DB_URL=jdbc:postgresql://localhost:5432/aisha \
DB_USERNAME=aisha \
DB_PASSWORD=aisha \
./mvnw spring-boot:run
```

## Database and Initialization

- The schema is automatically created/updated at startup via Hibernate (`ddl-auto: update`).
- Initial seed data is loaded from `src/main/resources/data.sql` in embedded mode.

## Tests

To run the test suite:

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

## Main Web Endpoints

- `/` redirects to `/dashboard`
- `/login`
- `/logout` (POST)
- `/dashboard`
- `/entries`
- `/accounts`
- `/categories`

## License

This project is licensed under the terms defined in `LICENSE`.
