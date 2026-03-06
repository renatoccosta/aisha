# Security Architecture

This document describes the current security architecture in AI$HA, including the local-session baseline, federated login integration, and the near-term OAuth2 evolution path.

## Goals and Principles

- Keep authentication and authorization auditable and predictable.
- Use secure defaults for sessions, cookies, and CSRF.
- Avoid provider lock-in while keeping OAuth2/OIDC compatibility.
- Preserve clear package boundaries (`security`, `web`, `infrastructure`).

## Current Model (March 2026)

- Primary app security is stateful: Spring Security with server-side sessions (`JSESSIONID`).
- Login methods:
  - Form login (`GET /login`, `POST /login`) against `local_user_accounts`.
  - OIDC login (`/oauth2/authorization/{registrationId}`), currently with Google registration configured.
- Route policy: authenticated-by-default; only a strict public allowlist is open.
- CSRF is enabled globally.
- Session hardening:
  - Session fixation protection via session migration.
  - Idle timeout: 45 minutes (`server.servlet.session.timeout`).
  - Absolute timeout: 12 hours (`AbsoluteSessionTimeoutFilter`).
  - Maximum concurrent sessions per user: 1 (new login replaces old session).

## Core Components

- `SecurityConfig`
  - Defines `SecurityFilterChain`, URL authorization rules, form login, oauth2 login, logout, CSRF, and custom filters.
- `DatabaseUserDetailsService`
  - Loads local users from `JpaLocalUserAccountRepository`.
  - Produces `AishaPrincipal` for local auth.
- `AishaOidcUserService`
  - Resolves/creates local account for a federated login and returns the unified `AishaPrincipal`.
- `FederatedAuthenticationService`
  - Persists and resolves provider subject mappings in `federated_user_identities`.
  - Handles account-link-required behavior when email already exists locally.
- `FederatedAuthenticationFailureHandler`
  - Intercepts `account_link_required` auth errors.
  - Stores pending link state in session and redirects to `/login?linkRequired`.
- `FederatedAuthController`
  - Confirms account linking with local password (`POST /auth/federated/link`).
  - Creates authenticated session context after successful confirmation.
- `AuditAuthenticationHandlers`
  - Logs authentication success/failure/logout.
  - Stores authentication timestamp in session for absolute timeout checks.
- `AbsoluteSessionTimeoutFilter`
  - Forces re-authentication after 12 hours from login time.
  - Supports HTMX redirect with `HX-Redirect` when session expires.
- `CorrelationIdFilter`
  - Accepts or generates `X-Correlation-Id`.
  - Stores it in MDC (`correlationId`) and response header.

## Authorization and Public Surface

Authenticated by default (`anyRequest().authenticated()`).

Public endpoints/paths:

- `/login`
- `/oauth2/**`
- `/login/oauth2/**`
- `/auth/federated/link`
- `/css/**`, `/js/**`, `/img/**`, `/webjars/**`
- `/error`, `/error/**`
- `/debug/**`
- `/favicon.ico`
- `/actuator/health`, `/actuator/health/**`

## Authentication Flows

### 1) Local Form Login

1. User requests `/login`.
2. User posts credentials to `POST /login`.
3. `DatabaseUserDetailsService` validates against `local_user_accounts`.
4. On success, `AuditAuthenticationHandlers` logs and redirects to `/dashboard`.
5. Session receives `SECURITY_AUTHENTICATED_AT`.

### 2) Federated OIDC Login

1. User starts at `/oauth2/authorization/{provider}`.
2. `AishaOidcUserService` loads identity claims.
3. `FederatedAuthenticationService`:
  - Reuses existing `(provider, subject)` link when present.
  - Creates local account and identity link when email is new.
  - Requires explicit local-password confirmation when email already exists.
4. On success, user is authenticated as `AishaPrincipal` and redirected to `/dashboard`.

### 3) Federated Account Linking (Existing Local Email)

1. OIDC login fails with internal `account_link_required`.
2. `FederatedAuthenticationFailureHandler` stores `SECURITY_PENDING_FEDERATED_LINK` in session.
3. Login page shows link confirmation form.
4. `POST /auth/federated/link` validates local password and persists provider link.
5. Controller establishes authenticated session context and redirects to `/dashboard`.

## Session and Cookie Policy

- Session creation policy: `IF_REQUIRED`.
- Session fixation protection: enabled (`migrateSession()`).
- Max sessions per user: `1`, newest login invalidates previous (`maxSessionsPreventsLogin(false)`).
- Cookie defaults:
  - `HttpOnly=true`
  - `SameSite=Lax`
  - `Secure=false` by default, `true` in `prod` profile.

## CSRF Policy

- CSRF remains enabled with Spring defaults.
- HTML/HTMX forms include CSRF hidden fields.
- This applies to local login, logout, and federated link confirmation POST flows.

## Persistence Model

Security-related tables:

- `local_user_accounts`
  - `id`, `username` (unique), `password_hash`, `enabled`
  - Source migration: `V1__init_schema.sql`
- `federated_user_identities`
  - `local_user_id` FK -> `local_user_accounts.id`
  - `provider`, `subject` unique pair, `email`, `created_at`
  - Source migration: `V7__federated_identities.sql`

Bootstrap user:

- `LocalUserAccountSeeder` creates `admin/admin` only when user does not exist.
- Controlled by:
  - `aisha.security.seed.username`
  - `aisha.security.seed.password`

## Logging, Auditing, and Incident Traceability

- Audit events:
  - Authentication success
  - Authentication failure
  - Logout success
  - No password logging
- Correlation ID:
  - Request-scoped and returned in `X-Correlation-Id`
  - Included in logging pattern via MDC key `correlationId`
- Error handling:
  - Unhandled errors are logged with stack trace, method/path, and correlation ID (`GlobalExceptionHandler`).
  - Business 400/404 flows in controllers log correlation ID and request metadata.

## Configuration Map

Main keys:

- `server.servlet.session.timeout` (idle session timeout)
- `server.servlet.session.cookie.http-only`
- `server.servlet.session.cookie.same-site`
- `server.servlet.session.cookie.secure`
- `aisha.security.seed.username`
- `aisha.security.seed.password`
- `spring.security.oauth2.client.registration.*`

## OAuth2 Evolution Path

Current code already supports OAuth2 client/OIDC login. To evolve without rework:

- Keep `AishaPrincipal` as the unified application principal abstraction.
- Keep provider link data in `federated_user_identities` and avoid provider-specific domain coupling.
- Add Resource Server support incrementally for API use cases:
  - JWT/Opaque token validation for non-browser clients.
- Maintain session-based MVC security for server-rendered web flows.
- Introduce authorization rules by role/scope only when required by concrete use cases.
