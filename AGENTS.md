# AGENTS.md — AI$HA (Personal Finance + AI)

## Context
AI$HA is a personal finance manager with AI features. Prioritize correctness, auditability, and safe defaults.

## Stack
- Java 25 (LTS) + Spring Boot 4.0.2
- Build: Maven
- Dev DB: HSQLDB
- Prod DB: PostgreSQL
- DB schema must be created automatically on startup (keep this working).
- Flyway is the mandatory database migration tool for schema evolution.
- Do not use Hibernate `ddl-auto` for schema creation or updates in normal development flow; use versioned migrations.

## Security / Auth direction
- Assume OAuth2 compatibility as a hard requirement.
- Avoid committing to a specific provider too early.
- Prefer designs that can support:
  - Resource Server (JWT/Opaque token validation)
  - OAuth2 Client when needed (calling external APIs)

### Current local-auth baseline (already implemented)
- Authentication mechanism: Spring Security form login with server-side session (`JSESSIONID`).
- Login/Logout routes:
  - `GET /login` (login page)
  - `POST /login` (authentication)
  - `POST /logout` (session invalidation)
- Route protection: all routes require authentication except `/login` and static assets.
- Local user storage:
  - Table: `local_user_accounts`
  - Seed user on startup: `admin/admin` (password stored as hash, never plain text)
  - Config keys: `aisha.security.seed.username`, `aisha.security.seed.password`
- Password hashing: BCrypt.
- Session hardening:
  - session fixation protection enabled (session id migration after login)
  - idle timeout: 45 minutes
  - absolute timeout: 12 hours (custom filter)
  - max concurrent sessions per user: 1 (new login replaces previous session)
- Cookie/session settings:
  - HttpOnly enabled
  - SameSite=Lax
  - Secure=true in `prod` profile
- CSRF:
  - enabled globally
  - all POST forms (including HTMX flows) must include CSRF token
- Audit logs (minimum):
  - authentication success
  - authentication failure
  - logout success
  - never log passwords


## Logging standards
- Use SLF4J as the logging API in application code.
- Use Logback as the logging implementation via Spring Boot support (prefer `application.yaml` logging properties).
- Keep logging configuration managed by Spring Boot conventions and profiles; prefer `application.yaml` over custom `logback-spring.xml` when requirements are simple.
- Log output must go to console only by default (do not add file appenders unless explicitly requested).
- Use correlation IDs for request tracing:
  - Accept incoming `X-Correlation-Id` when present; otherwise generate one.
  - Propagate correlation ID to MDC key `correlationId` and response header `X-Correlation-Id`.
- Any error returned to users must generate a technical log entry including:
  - exception type and stack trace
  - request metadata (method/path)
  - correlation ID
- Add informative logs at key execution points that improve operability, while avoiding sensitive data (passwords, tokens, secrets).

## Front-end architecture
- Server-side rendered HTML using Spring MVC.
- Use HTMX for interactivity (partial page updates, forms, tables).
- Prefer HTML fragments over JSON for browser-facing endpoints.
- REST/JSON endpoints are allowed for:
  - Integrations
  - Future SPA or mobile clients
  - Non-UI use cases
  
## UI / UX standards (Web)
- The UI must be fully responsive (mobile-first).
- In listing screens (entries, categories, accounts, and future list pages), when columns do not fit viewport width, render rows as stacked cards (multi-line content) instead of requiring horizontal scrolling.
- In listing screens (entries, categories, accounts, local users, and future CRUD list pages), pagination is mandatory by default:
  - Server-side pagination only (database-driven with LIMIT/OFFSET or equivalent), never loading full datasets in the browser just to paginate.
  - Default page size: 25; allowed sizes: 50 and 100.
  - Required controls: first page, previous, next, and last.
  - Required indicators: current page, total pages, current page record range, and total records.
- Use a modern visual style:
  - consistent spacing scale
  - clear typography hierarchy (h1/h2/body)
  - accessible contrast and focus states
  - cohesive component styling (buttons, inputs, tables, cards)
- Prefer simple, reusable components via template fragments (header, navbar, card, form controls).
- Avoid custom JavaScript unless strictly necessary; use HTMX for interactivity.
- Validate user input both client-side (when easy) and server-side (always).
- Iconography is mandatory and must be consistent:
  - Use Lucide icons (https://lucide.dev) as the default icon set for all current and future implementations.
  - Every button with visible text must show a representative icon on the left.
  - Ignore buttons that are icon-only by design.
  - Every listing table column header must show a representative icon on the left.
  - Every main navigation functionality (Dashboard, Entries, Categories, Accounts, and future items) must have a representative icon shown to the left of its name.
  - This must be the default behavior for new implementations.

## Controller guidelines
- Controllers may return full pages or HTML fragments.
- Business logic must stay in application/domain layers.
- Avoid coupling business rules to view templates.

## Engineering rules
- Keep diffs small and reviewable.
- Before changing multiple files: write a short plan and list files to touch.
- Add tests for non-trivial logic (unit tests first, then integration when needed).
- After changes: run `mvn test` (or explain why it wasn’t possible).

## Architecture preferences
- Clear package boundaries (domain / application / infrastructure / web).
- Constructor injection; minimize mutable state.
- Prefer explicit, typed DTOs; validate inputs at boundaries.
- Avoid premature frameworks/abstractions; keep it simple until requirements force complexity.

## Language conventions

### Source code language
- All source code MUST be written in English:
  - class names
  - method names
  - variable names
  - package names
  - database table/column names
  - log messages
  - comments
- Do not mix Portuguese terms in code unless explicitly requested.

### User interface language
- All user-facing text must be in Brazilian Portuguese (pt-BR), with correct accentuation.
- UI labels, messages, buttons, validation messages and page titles must be in pt-BR.
- Avoid hardcoded strings in templates when possible; prepare for future i18n support.

### Documentation language
- All project documentation must be written in English.
- This includes `README.md`, files under `docs/`, ADRs, runbooks, and any newly created Markdown documentation.

### Formatting standards
- Monetary values must follow Brazilian formatting in the UI:
  - Decimal separator: comma
  - Thousands separator: dot
  - Currency symbol: R$ (when applicable)
- Internally, monetary values must use BigDecimal.

## Data & money rules (very important)
- Never silently change monetary values (currency, scale, rounding).
- Prefer BigDecimal for money amounts.
- Explicit rounding mode whenever rounding is needed.
- Keep a clear audit trail for derived values.

## Safety rails for Codex
- Do not run destructive commands.
- Ask before adding major dependencies or changing Maven build structure.
- Do not commit secrets or tokens; use env vars / local config.
