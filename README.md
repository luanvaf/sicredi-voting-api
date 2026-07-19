# Sicredi Voting API

A production-minded Java 21 and Spring Boot implementation of Sicredi's backend
voting challenge.

The API creates agendas, opens time-bound voting sessions, validates associate
eligibility, accepts one vote per associate, calculates immutable results, and
publishes finalized results to RabbitMQ through a transactional outbox.

## Delivered Scope

The mandatory challenge and every proposed bonus are included:

- agenda creation;
- configurable voting sessions with a 60-second default;
- `YES` and `NO` votes with database-enforced uniqueness;
- finalized vote counts and outcome;
- external CPF eligibility integration;
- API versioning under `/api/v1`;
- reliable RabbitMQ result publication;
- a configurable 100,000-vote k6 scenario;
- OpenAPI documentation, health probes, Docker, CI, and free-tier deployment
  assets.

The external eligibility URL from the original challenge is no longer
available. A deterministic Cloudflare Worker substitute is included so the
integration remains reproducible without weakening the HTTP boundary.

## Technology

- Java 21
- Spring Boot 4.0
- Spring MVC, Data JPA, JDBC, AMQP, Validation, and Actuator
- PostgreSQL 17 and Flyway
- RabbitMQ 4.1 with publisher confirms
- springdoc OpenAPI
- JUnit 6, AssertJ, Mockito, ArchUnit, WireMock, and Testcontainers
- JaCoCo, Spotless, Docker, GitHub Actions, and k6

## Run with Docker Compose

Requirements:

- Docker Engine or Docker Desktop
- Docker Compose v2

Create the ignored local environment file and set unique passwords:

```powershell
Copy-Item .env.example .env
```

Then start the complete local topology:

```shell
docker compose up --build --detach
docker compose ps
```

The stack contains the API, PostgreSQL, RabbitMQ, and an HTTP eligibility
service using the same contract as the hosted Worker.

Open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>
- RabbitMQ management: <http://localhost:15672>

Stop the stack without deleting its data:

```shell
docker compose down
```

## Run from Maven

Start PostgreSQL, RabbitMQ, and an eligibility provider, then configure the
connection variables documented in `.env.example`.

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```shell
./mvnw spring-boot:run
```

Flyway validates and migrates the database at startup. Hibernate only validates
the resulting schema.

## API

All application endpoints are versioned.

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/agendas` | `201` | Create an agenda |
| `POST` | `/api/v1/agendas/{agendaId}/voting-sessions` | `201` | Open the agenda's only session |
| `POST` | `/api/v1/agendas/{agendaId}/votes` | `201` | Cast an eligible associate's vote |
| `GET` | `/api/v1/agendas/{agendaId}/result` | `200` | Finalize an expired session and read its result |

Create an agenda:

```http
POST /api/v1/agendas
Content-Type: application/json

{
  "title": "2027 investment plan",
  "description": "Approve the proposed annual investment plan"
}
```

Open a session. The body may be omitted to use the 60-second default:

```http
POST /api/v1/agendas/{agendaId}/voting-sessions
Content-Type: application/json

{
  "durationSeconds": 300
}
```

Cast a vote:

```http
POST /api/v1/agendas/{agendaId}/votes
Content-Type: application/json

{
  "associateCpf": "93541134780",
  "choice": "YES"
}
```

Read the result after finalization:

```json
{
  "agendaId": "1644826d-60a8-4233-b0ce-5df032d66620",
  "votingSessionId": "bb3b97b9-7505-468f-895b-900ed999071d",
  "yesVotes": 1,
  "noVotes": 0,
  "totalVotes": 1,
  "outcome": "APPROVED",
  "closedAt": "2026-07-19T17:00:00Z",
  "finalizedAt": "2026-07-19T17:00:00.250Z"
}
```

Errors use RFC 9457 `application/problem+json` responses with a stable `code`.
The main mappings are:

- `400` for malformed input, invalid UUIDs, invalid CPF check digits, or invalid
  durations;
- `404` when an agenda does not exist;
- `409` for an existing session, a closed session, a duplicate vote, or a
  result that is not finalized yet;
- `422` when the eligibility provider does not know the associate or rejects
  voting eligibility;
- `503` when PostgreSQL or the eligibility provider is unavailable.

## Business Decisions

- An agenda has at most one voting session.
- A requested duration must be between one second and 24 hours.
- A vote is accepted in the half-open interval
  `openedAt <= castAt < closesAt`.
- The exact closing instant is already closed.
- No partial result is exposed.
- More `YES` votes means `APPROVED`; more `NO` votes means `REJECTED`; equal
  counts, including zero to zero, mean `TIED`.
- CPF input contains exactly 11 unformatted digits and valid check digits.

These decisions and the original ambiguities are recorded in
[ADR-0002](docs/architecture/adr/0002-define-voting-lifecycle.md).

## Architecture

The project uses a pragmatic Java adaptation of MASA with Clean Architecture
dependency inversion:

```text
domainmodel  <-  engine
     ^             ^
     +--------- service <--------- delivery
                    ^
                    |
               integration

bootstrap composes all layers without moving framework types inward.
```

- `domainmodel`: framework-free entities, value objects, and events;
- `engine`: deterministic business policies and calculations;
- `service`: use cases plus inbound and outbound ports;
- `integration`: PostgreSQL, external HTTP, and RabbitMQ adapters;
- `delivery`: versioned controllers and scheduled jobs;
- `bootstrap`: Spring configuration and dependency wiring.

ArchUnit enforces the inward dependencies of the business layers. The
architecture rationale and the differences from literal MASA are in
[ADR-0001](docs/architecture/adr/0001-adopt-masa-inspired-clean-architecture.md).

## Concurrency and Reliable Messaging

PostgreSQL is the final authority for concurrent invariants:

- one session per agenda is a unique constraint;
- one vote per session and CPF is a unique constraint;
- duplicate inserts use `ON CONFLICT DO NOTHING`, avoiding exception logs that
  could expose CPF;
- voting acquires a shared session lock and rechecks the closing boundary;
- finalization uses exclusive `FOR UPDATE SKIP LOCKED` claims;
- vote counts are aggregated in SQL.

Result, session finalization, and outbox event are committed atomically. The
relay claims pending rows with a fenced lease, publishes persistent JSON with
correlated broker confirms, and only then marks the event as published.

Delivery is at least once. A crash after RabbitMQ acknowledges a message but
before PostgreSQL records that acknowledgement can produce a duplicate.
Consumers must deduplicate using the event's `messageId`/`eventId`.

No CPF is included in result events or application logs.

## Verification

Run the same quality gate used by CI:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

The suite includes:

- domain, policy, and use-case unit tests;
- WireMock eligibility contract tests;
- PostgreSQL outbox lease and fencing tests;
- RabbitMQ publisher-confirm tests;
- ArchUnit dependency rules;
- a full PostgreSQL/RabbitMQ/WireMock API lifecycle;
- a 20-request concurrency race proving exactly one duplicate vote wins;
- an assertion that CPF values do not appear in captured application logs.

The expensive load scenario is outside the default Maven lifecycle. See
[performance/README.md](performance/README.md) for smoke and 100,000-vote
commands.

## Deployment

The Docker image is multi-stage, Java 21, non-root, read-only compatible, and
exposes an Actuator readiness health check.

`render.yaml` and [the deployment guide](docs/deployment.md) describe a
no-cost evaluation topology using:

- Render for the API;
- Neon for PostgreSQL;
- CloudAMQP for AMQP 0-9-1;
- Cloudflare Workers for eligibility.

Free plans sleep, have quotas, and are suitable for this evaluation rather than
production. The repository can remain private while Render is granted explicit
access.
