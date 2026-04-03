# Contributing to AI$HA

Thanks for your interest in contributing to **AI$HA**.

AI$HA is an open source, self-hosted personal finance platform with AI-assisted features. We prioritize correctness, auditability, security, and maintainability.

## Ways to Contribute

- Report bugs and usability issues
- Improve documentation
- Propose and implement features
- Add or improve tests
- Review pull requests

For security vulnerabilities, **do not open public issues**. Follow [SECURITY.md](SECURITY.md).

## Project Principles

When contributing, keep these priorities in mind:

- Safe defaults and secure-by-design behavior
- Clear and reviewable changes
- Strong test coverage for non-trivial logic
- Explicit handling of monetary values and persistence changes
- Good documentation for operational and developer workflows

## Development Setup

### Prerequisites

- JDK 25
- Maven 3.9+
- Git

### Run locally

```bash
./mvnw spring-boot:run
```

The default local profile uses HSQLDB. Database schema evolution must be handled via Flyway migrations.

### Run tests

```bash
./mvnw test
```

## Branching and Commits

- Create a topic branch from `main`
- Keep commits focused and small
- Use clear commit messages (Conventional Commits style is preferred)

Examples:

- `feat: add pagination controls for accounts list`
- `fix: preserve currency scale when importing entries`
- `docs: clarify self-hosted upgrade process`

## Pull Request Guidelines

Before opening a PR:

1. Rebase on the latest `main`
2. Run tests locally
3. Ensure docs are updated when behavior/configuration changes
4. Verify no secrets, tokens, or credentials are committed

In the PR description, include:

- **Motivation** (why this change is needed)
- **What changed** (high-level summary)
- **How to validate** (commands and manual steps)
- **Risks / compatibility notes** (if any)

## Code and Documentation Standards

- Source code, comments, logs, and identifiers must be in English
- User-facing UI text must be Brazilian Portuguese (pt-BR)
- Documentation files must be in English
- Avoid unnecessary abstractions; keep domain/application/infrastructure/web boundaries clear
- Use constructor injection and explicit DTO validation at boundaries

## Database and Migrations

- Use Flyway versioned migrations for all schema changes
- Do not rely on Hibernate `ddl-auto` for normal schema evolution
- Make migration scripts deterministic and reversible where practical
- Validate migration impact on both local and production-like flows

## Testing Expectations

- Add unit tests for non-trivial business logic
- Add integration tests when behavior depends on framework, database, or security wiring
- Keep tests deterministic and isolated

At minimum, contributors should run:

```bash
./mvnw test
```

## Local Environment & Self-Hosted Context

AI$HA is designed for self-hosted environments. When changing operational behavior, include:

- Environment variable updates
- Deployment notes (if applicable)
- Backward-compatibility considerations

Relevant references:

- [docs/development.md](docs/development.md)
- [docs/deploy-self-hosted.md](docs/deploy-self-hosted.md)
- [docs/operations.md](docs/operations.md)

## Review Criteria

PRs are typically reviewed for:

- Functional correctness
- Security impact
- Data integrity and migration safety
- Test coverage adequacy
- Readability and maintainability

Maintainers may request follow-up changes before merge.

## License

By contributing, you agree that your contributions are licensed under the repository license.
