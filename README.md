# AI$HA - Personal Finance + AI for Self-Hosted Environments

AI$HA is a personal finance application designed for self-hosted use, with AI-assisted workflows, traceability, and safe operational defaults.

## Purpose

AI$HA exists to help people and teams manage personal finance data with reliability, auditable behavior, and practical AI support, while keeping control of their own infrastructure and data.

## Core Features

- Financial entries with categorization and history tracking
- Account and category management
- Dashboard with balance, revenue, expense, and trend views
- Date-range filtering across analysis screens
- Local authentication with hardened session controls
- Optional OAuth2/OIDC-ready architecture
- AI-powered category suggestion with persisted model lifecycle

## Architecture Overview

AI$HA follows a layered architecture with clear boundaries between domain, application rules, infrastructure adapters, and web delivery.

This README keeps only the high-level view. Detailed technical design is documented here:

- Development guide: [docs/development.md](docs/development.md)
- Security architecture: [docs/security-architecture.md](docs/security-architecture.md)
- Front-end architecture: [docs/frontend-architecture.md](docs/frontend-architecture.md)
- AI architecture: [docs/ai-architecture.md](docs/ai-architecture.md)

## Self-Hosted Deployment

This section is for running AI$HA as a finished product in your own environment.

### Common Requirements

- Access to the application package (release JAR) or container image
- PostgreSQL for persistent environments
- Runtime configuration for database connection and admin seed credentials

Database schema creation and evolution are handled automatically at startup by Flyway migrations.

### Option 1: Manual Runtime Setup

Use this when you want full control over the runtime environment.

1. Install Java 25 (JRE or JDK) and PostgreSQL.
2. Create a PostgreSQL database and user for AI$HA.
3. Download the latest release JAR artifact.
4. Configure environment variables (minimum):
   - `SPRING_PROFILES_ACTIVE=postgres,prod`
   - `DB_URL=jdbc:postgresql://<host>:5432/<database>`
   - `DB_USERNAME=<db-user>`
   - `DB_PASSWORD=<db-password>`
   - `AISHA_SECURITY_SEED_USERNAME=<admin-user>`
   - `AISHA_SECURITY_SEED_PASSWORD=<admin-password>`
5. Start the application:

```bash
java -jar app.jar
```

6. Access the app at `http://<host>:8080`.

### Option 2: Docker (Recommended for Most Cases)

Use the published image and optionally the repository `compose.yml`.

1. Set the image/tag and credentials in `compose.yml`.
2. Start the stack:

```bash
docker compose up -d
```

3. Access the app at `http://localhost:8080`.

For full container deployment details, image distribution, and upgrade flow, see:

- [docs/deploy-self-hosted.md](docs/deploy-self-hosted.md)
- [docs/operations.md](docs/operations.md)

## Contributing

Contributions are welcome. For local development setup and technical workflow:

1. Install JDK 25.
2. Run the app locally (embedded database):

```bash
./mvnw spring-boot:run
```

3. Run tests before opening a PR:

```bash
./mvnw test
```

Full contributor-oriented documentation:

- [docs/development.md](docs/development.md)

## Additional References

- Design system: [docs/design-system.md](docs/design-system.md)
- Privacy policy (EN): [docs/legal/privacy-policy.md](docs/legal/privacy-policy.md)
- Privacy policy (pt-BR): [docs/legal/privacy-policy_pt_BR.md](docs/legal/privacy-policy_pt_BR.md)
- Terms of use (EN): [docs/legal/terms-of-use.md](docs/legal/terms-of-use.md)
- Terms of use (pt-BR): [docs/legal/terms-of-use_pt_BR.md](docs/legal/terms-of-use_pt_BR.md)
- License: [LICENSE](LICENSE)
