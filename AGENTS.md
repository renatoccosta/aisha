# AGENTS.md — AI$HA (Personal Finance + AI)

## Context
AI$HA is a personal finance manager with AI features. Prioritize correctness, auditability, and safe defaults.

## Stack
- Java 25 (LTS) + Spring Boot 4.0.2
- Build: Maven
- Dev DB: HSQLDB
- Prod DB: PostgreSQL
- DB schema must be created automatically on startup (keep this working).

## Security / Auth direction
- Assume OAuth2 compatibility as a hard requirement.
- Avoid committing to a specific provider too early.
- Prefer designs that can support:
  - Resource Server (JWT/Opaque token validation)
  - OAuth2 Client when needed (calling external APIs)

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
- Use a modern visual style:
  - consistent spacing scale
  - clear typography hierarchy (h1/h2/body)
  - accessible contrast and focus states
  - cohesive component styling (buttons, inputs, tables, cards)
- Prefer simple, reusable components via template fragments (header, navbar, card, form controls).
- Avoid custom JavaScript unless strictly necessary; use HTMX for interactivity.
- Validate user input both client-side (when easy) and server-side (always).

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
