# AI$HA - Agentic Inteligence for Self-Hosted Accounting

AI$HA is a personal finance manager with AI capabilities, focused on correctness, traceability, and safe architectural evolution.

## Project Goals

- Help users manage personal finances with clear and reliable data.
- Provide a practical foundation for AI-powered financial insights.
- Preserve auditability and predictable behavior as the system evolves.

## Main Features

- Financial entry management (`entries`)
- Account management (`accounts`)
- Category management (`categories`)
- Global date-range filter for analysis
- Dashboard with financial summaries and charts
- Local authentication baseline with secure server-side sessions
- Persisted AI-powered entry category suggestion with background retraining

## Architecture Overview

AI$HA follows a layered architecture with clear boundaries:

- `domain`: entities and repository contracts
- `application`: use cases and business rules
- `infrastructure`: persistence and technical adapters
- `web`: MVC controllers, forms, and server-side HTML rendering

Frontend uses Spring MVC + Thymeleaf with HTMX for interactive flows.

For full technical architecture, security details, data initialization, and endpoint details, see:

- [docs/development.md](docs/development.md)
- [docs/security-architecture.md](docs/security-architecture.md)

## Running the Application

### Development mode (embedded HSQLDB)

```bash
./mvnw spring-boot:run
```

Application URL: `http://localhost:8080`

### PostgreSQL profile

```bash
SPRING_PROFILES_ACTIVE=postgres \
DB_URL=jdbc:postgresql://localhost:5432/aisha \
DB_USERNAME=aisha \
DB_PASSWORD=aisha \
./mvnw spring-boot:run
```

For deployment, Docker/GHCR usage, and release workflows, see:

- [docs/operations.md](docs/operations.md)
- [docs/deploy-self-hosted.md](docs/deploy-self-hosted.md)

## Contributing

- Keep changes small and reviewable.
- Follow migration-first database changes with Flyway.
- Add tests for non-trivial logic.
- Run tests locally before opening a PR:

```bash
./mvnw test
```

Additional references:

- Development details: [docs/development.md](docs/development.md)
- Security architecture: [docs/security-architecture.md](docs/security-architecture.md)
- AI architecture and category suggestion: [docs/ai-architecture.md](docs/ai-architecture.md)
- Front-end architecture: [docs/frontend-architecture.md](docs/frontend-architecture.md)
- Design system: [docs/design-system.md](docs/design-system.md)
- Self-hosted deployment: [docs/deploy-self-hosted.md](docs/deploy-self-hosted.md)

## License

This project is licensed under the terms defined in [LICENSE](LICENSE).
