# ADR-0001: Adopt a MASA-Inspired Clean Architecture

- Status: Accepted
- Date: 2026-07-19

## Context

The application has a small business domain but several infrastructure concerns: PostgreSQL persistence, an external eligibility API, RabbitMQ result publication, scheduled session closure, and high-concurrency voting.

The solution must remain easy to understand and change without mixing business policy with framework or transport details. It must also avoid architecture ceremony that does not protect a real boundary.

The Modular Agentic Semantic Architecture (MASA) framework emphasizes semantic naming, explicit dependencies, pure engines, boundary data mapping, thin delivery handlers, and infrastructure isolation. Its published language guides do not currently include Java, and its original dependency hierarchy lets services import integration contracts.

## Decision

Use MASA cognizability principles with a Java and Spring adaptation that preserves Clean Architecture dependency inversion.

The production packages are:

- `domainmodel`: framework-free business vocabulary.
- `engine`: pure policies and calculations.
- `service`: use cases and service-owned inbound and outbound ports.
- `integration`: persistence, HTTP, and messaging adapters.
- `delivery`: HTTP and scheduling entry points.
- `bootstrap`: Spring Boot startup, configuration, and dependency wiring.

Outbound ports belong to the service layer. Integration adapters implement those ports. This deliberately differs from the original MASA hierarchy so business workflows do not depend on infrastructure packages.

Spring may resolve dependencies, but every dependency must remain visible through an explicit constructor. Field injection and generated constructors are forbidden.

Engines are created only for meaningful pure behavior. A feature that only orchestrates ports does not receive an artificial engine.

## Enforced Rules

- Domain models contain no Spring, JPA, Jackson, HTTP, or AMQP types.
- Engines depend only on domain models and perform no I/O.
- HTTP DTOs, JPA entities, and external payloads are mapped at their boundaries.
- Infrastructure-specific exceptions do not escape their adapters.
- Controllers validate, map, dispatch, and map the result.
- Database uniqueness constraints protect concurrent voting invariants.
- ArchUnit tests enforce dependency direction.

## Consequences

### Positive

- Business rules remain deterministic and inexpensive to test.
- External systems can be replaced without changing use cases.
- The package tree exposes business intent and architectural responsibility.
- Dependency violations are detectable automatically.

### Trade-offs

- Boundary mapping creates more types than a conventional Spring CRUD service.
- Explicit constructors and typed identifiers add code.
- The project includes a `bootstrap` area beyond MASA's five original layers.

These costs are accepted only where they protect a real domain or infrastructure boundary.

## References

- [MASA Skill](https://github.com/nicolasfmelo/masa-skill)
- [MASA Framework and experiments](https://github.com/nicolasfmelo/masa-framework)
- [MASA rulesets](https://github.com/nicolasfmelo/masa-skill/blob/main/references/rulesets.md)
