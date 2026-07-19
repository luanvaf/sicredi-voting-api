# Project Instructions

## Language

- Use English for source code, identifiers, API contracts, errors, logs, documentation, branch names, and commit messages.
- Preserve the business vocabulary defined in the domain model. Do not introduce Portuguese aliases.

## Architecture

This project follows a pragmatic Java adaptation of MASA combined with Clean Architecture dependency inversion.

- `domainmodel` contains framework-free entities, value objects, domain events, and business errors.
- `engine` contains pure, deterministic business policies and calculations.
- `service` contains use cases and declares outbound ports.
- `integration` implements outbound ports for persistence, HTTP, and messaging.
- `delivery` contains inbound adapters such as HTTP controllers and scheduled jobs.
- `bootstrap` contains Spring Boot configuration and dependency wiring.

Dependencies must point toward business policy:

- `domainmodel` depends only on the Java standard library.
- `engine` depends only on `domainmodel`.
- `service` depends only on `domainmodel`, `engine`, and service-owned ports.
- `integration` may depend on service ports and `domainmodel`.
- `delivery` may depend on service input contracts and delivery DTOs.
- `bootstrap` may depend on every layer only for configuration and wiring.

Do not create an engine when a use case has no meaningful pure business rule.

## Java and Spring

- Target Java 21.
- Prefer records for immutable value objects and commands when their invariants remain explicit.
- Use explicit constructor injection. Do not use field injection or Lombok-generated constructors.
- Keep Spring, JPA, Jackson, HTTP, and AMQP types out of `domainmodel` and `engine`.
- Map HTTP DTOs, persistence entities, and external API payloads at their boundaries.
- Use UTC timestamps and inject `Clock` into time-dependent business flows.
- Translate infrastructure failures at adapter boundaries into stable application errors.
- Never log full CPF values or credentials.

## Persistence and Concurrency

- Flyway owns the database schema. Hibernate schema generation must remain disabled.
- Database constraints are the final authority for uniqueness and concurrency invariants.
- Do not load all votes into memory to calculate results; aggregate in PostgreSQL.
- Result publication uses a transactional outbox and provides at-least-once delivery.

## Testing

- Name focused unit tests with the `Test` suffix.
- Name container-backed tests with the `IntegrationTest` suffix.
- Name dependency-rule tests with the `ArchitectureTest` suffix.
- Prefer deterministic tests without sleeps. Use Awaitility only for genuinely asynchronous boundaries.
- Use Testcontainers for PostgreSQL and RabbitMQ and WireMock for external HTTP behavior.
- Keep expensive performance scenarios outside the default Maven test lifecycle.

## Git

- Use Conventional Commits in English.
- Keep commits atomic and independently reviewable.
- Use only the repository-configured Git author.
- Never add `Co-authored-by` trailers.
- Never create branches with a `codex` prefix.
- Keep the GitHub repository private until the owner explicitly approves making it public.
