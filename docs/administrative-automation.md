# Administrative Automation

This document records the recommended structure for running administrative system functions, starting with system backup execution, from non-browser clients such as scripts and command-line tools.

## Context

AI$HA currently exposes system backup execution through the server-rendered administration UI. The backup behavior itself already lives in the application layer, primarily through `SystemBackupCoordinator` and related backup services.

Future administrative functions may need to be executed by automation, for example:

- Scheduled backup jobs.
- Operational maintenance commands.
- Administrative scripts.
- CI or deployment hooks.

The implementation should avoid duplicating business rules across web controllers, command-line commands, and background utilities.

## Recommendation

Use a single application-layer use case for each administrative operation, then expose it through two thin adapters:

- An administrative HTTP API for automation.
- A CLI that acts as a client of that API.

The API should be the stable integration contract. The CLI should handle local configuration, authentication, request execution, terminal output, and exit codes, but it should not reimplement business logic.

## Why Not Only REST

An administrative REST API is the right foundation for automation because it is:

- Easy to call from scripts, schedulers, CI jobs, and external tools.
- Compatible with future non-browser clients.
- Naturally aligned with OAuth2 Resource Server support.
- Easier to secure consistently through Spring Security.

However, using REST alone leaves operators with raw `curl` commands and manual token handling. A CLI improves ergonomics without changing the underlying architecture.

## Why Not Only CLI

A CLI that executes the backup directly in a separate JVM process would create operational risks.

The current backup coordination is process-local: it tracks status in memory and blocks data-changing web requests while a backup is running. If a separate CLI process ran backup logic directly, the running web application might not know that a backup is active unless the system introduced a shared persistent lock.

For online backups, the safer model is:

1. The CLI authenticates to the running AI$HA instance.
2. The CLI requests backup execution through the administrative API.
3. The running application coordinates the backup and enforces its write-blocking behavior.

An offline direct-execution CLI mode could exist later, but it should be explicit and separate. For example, a future `aisha maintenance backup-offline` command could require the main application to be stopped or could rely on a database-backed lock.

## Proposed HTTP Surface

Initial endpoints can be introduced under an administrative API namespace, for example:

- `POST /api/admin/system-backup/jobs`
  - Starts a backup job when none is running.
- `GET /api/admin/system-backup/jobs/current`
  - Returns the latest backup status.
- `GET /api/admin/system-backup/download`
  - Downloads the latest available backup archive.

The existing MVC/HTMX administration UI can continue to exist independently. Both the HTML controller and the API controller should call the same application-layer services.

## Proposed CLI Surface

A future CLI can expose commands such as:

```bash
aisha backup run
aisha backup status
aisha backup download
```

The CLI responsibilities should be limited to:

- Reading configuration such as base URL and token location.
- Sending authenticated HTTP requests.
- Rendering human-readable terminal output.
- Returning meaningful exit codes.
- Avoiding direct access to application internals or database tables.

## Authentication Model

Do not use username and password authentication for CLI automation.

The recommended short-term model is an automation token using:

```http
Authorization: Bearer <token>
```

These tokens should be opaque application tokens managed by AI$HA. Only a hash of each token should be stored in the database.

Each token should have at least:

- A generated secret value shown only once.
- A stored hash of the secret.
- A display name or description.
- An owner user or service account.
- Explicit scopes, such as `backup:run`, `backup:read`, and `backup:download`.
- Optional expiration.
- Revocation state.
- Created timestamp.
- Last-used timestamp.

Logs must never include the token value. Token usage should still be auditable through metadata such as owner, token id, scope, request path, and correlation id.

## OAuth2 Compatibility

The automation-token model should be designed as an incremental step toward Resource Server support.

Short term:

- Accept AI$HA-managed opaque bearer tokens for administrative API calls.
- Resolve tokens into the same application principal abstraction used elsewhere, such as `AishaPrincipal`.
- Authorize operations by token scopes.

Long term:

- Add OAuth2 Resource Server support for JWT or opaque token validation.
- Allow an external authorization server to issue tokens.
- Keep the same API authorization model based on principals and scopes.

This keeps the system compatible with OAuth2 without forcing an immediate dependency on a specific identity provider.

## Authorization

Administrative automation should use explicit permissions. Avoid treating every authenticated API caller as an administrator.

Recommended initial scopes:

- `backup:run`
- `backup:read`
- `backup:download`

Future administrative functions should add similarly narrow scopes instead of reusing broad permissions.

## Operational Notes

- API requests should participate in the existing correlation-id flow.
- Administrative API failures should be logged with exception details, request metadata, and correlation id.
- State-changing administrative API endpoints should not depend on browser CSRF tokens when authenticated with bearer tokens.
- Browser MVC endpoints should keep the existing session and CSRF behavior.
- Backup execution should remain coordinated by the running application for online use cases.

## Decision Summary

The preferred architecture is:

1. Keep administrative business behavior in application-layer services.
2. Add administrative HTTP APIs as the integration contract.
3. Build the CLI as a client of those APIs.
4. Authenticate CLI and automation through scoped bearer tokens, not username/password.
5. Keep the model compatible with future OAuth2 Resource Server support.
