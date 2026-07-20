# Sicredi Voting API

A production-minded Java 21 and Spring Boot implementation of Sicredi's backend
voting challenge.

The API creates agendas, opens time-bound voting sessions, validates associate
eligibility, accepts one vote per associate, calculates immutable results, and
publishes finalized results to RabbitMQ through a transactional outbox.

## Important: Eligibility Test Data

The external eligibility service URL supplied in the challenge PDF is no
longer available. The project therefore includes a deterministic Cloudflare
Worker substitute that preserves the original HTTP contract.

The PDF defines both `ABLE_TO_VOTE` and `UNABLE_TO_VOTE` for valid CPFs, but it
does not specify the business rule that determines eligibility. To avoid
rejecting an arbitrary portion of valid CPFs, the substitute uses the PDF's own
examples as explicit test fixtures:

| CPF | Simulated eligibility |
| --- | --- |
| `19839091069` | `ABLE_TO_VOTE` |
| `62289608068` | `UNABLE_TO_VOTE` |
| Any other mathematically valid CPF | `ABLE_TO_VOTE` |

Use `19839091069` for the successful Swagger voting flow and `62289608068` to
verify the `422 ASSOCIATE_NOT_ELIGIBLE` response. Invalid, formatted, or
repeated-digit CPFs are rejected instead of being treated as eligible. This
fixture mapping exists only because the original dependency is unavailable; it
is not a real business eligibility rule.

## Live Deployment

- Swagger UI: <https://sicredi-voting-api.onrender.com/swagger-ui.html>
- OpenAPI document: <https://sicredi-voting-api.onrender.com/v3/api-docs>
- Health: <https://sicredi-voting-api.onrender.com/actuator/health>
- Eligibility service health:
  <https://sicredi-eligibility-worker.luanvaf.workers.dev/health>

The hosted API uses free-tier infrastructure. Its first request after an idle
period can take about a minute while Render and Neon resume their resources.

## Domain Vocabulary

The implementation keeps the challenge's business meaning while using
consistent English terminology:

| Challenge term | API term |
| --- | --- |
| Pauta | Agenda |
| Sessão de votação | Voting session |
| Associado | Associate |
| Voto `Sim` / `Não` | Vote `YES` / `NO` |

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

The deterministic substitute described above keeps the discontinued external
integration reproducible without weakening the HTTP boundary.

## Technology

- Java 21
- Spring Boot 4.0
- Spring MVC, Data JPA, JDBC, AMQP, Validation, and Actuator
- PostgreSQL 17 and Flyway
- RabbitMQ 4.1 with publisher confirms
- springdoc OpenAPI
- JUnit 6, AssertJ, Mockito, ArchUnit, WireMock, and Testcontainers
- JaCoCo, Spotless, Docker, GitHub Actions, and k6

## Quick Start with Docker Compose

Docker Compose is the recommended way to run the complete application locally.

Requirements:

- Docker Engine or Docker Desktop
- Docker Compose v2

Create the ignored local environment file.

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

On Linux or macOS:

```shell
cp .env.example .env
```

Edit `.env` and set non-empty local values for both password fields:

```dotenv
POSTGRES_PASSWORD=change-me-postgres
RABBITMQ_PASSWORD=change-me-rabbitmq
```

Use unique values, keep the file local, and never commit it. Docker Compose
rejects empty passwords deliberately.

Build and start the complete local topology:

```shell
docker compose up --build --detach
docker compose ps
```

The stack contains the API, PostgreSQL, RabbitMQ, and an HTTP eligibility
service using the same contract as the hosted Worker. The first build downloads
container images and Maven dependencies, so it can take several minutes.
Compose starts the API only after its dependencies are healthy.

Run `docker compose ps` again until all four services report `healthy`. If the
API does not become healthy, follow its startup logs:

```shell
docker compose logs --follow app
```

Open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>
- RabbitMQ management: <http://localhost:15672>

RabbitMQ management uses `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD` from the
local `.env` file.

Stop the stack without deleting its data:

```shell
docker compose down
```

To delete the local PostgreSQL and RabbitMQ data as well:

```shell
docker compose down --volumes
```

The command with `--volumes` is destructive and should only be used when the
local data can be discarded.

### Verify the API in Swagger

After every service is healthy:

1. Open <http://localhost:8080/swagger-ui.html>.
2. Create an agenda with `POST /api/v1/agendas`, using a title and optional
   description, then copy its `id`.
3. Open its session with
   `POST /api/v1/agendas/{agendaId}/voting-sessions` and
   `{"durationSeconds": 5}`.
4. Cast a vote with `POST /api/v1/agendas/{agendaId}/votes`, using the valid
   eligible payload `{"associateCpf": "19839091069", "choice": "YES"}`.
5. Wait for the configured session duration and read the finalized result with
   `GET /api/v1/agendas/{agendaId}/result`.

## Run from Maven

This alternative requires Java 21 and a local Node.js installation with `npx`.
The commands below assume the default ports and usernames from `.env`.

First create `.env` as described in the Docker Compose quick start and run
PostgreSQL and RabbitMQ:

```shell
docker compose up --detach postgres rabbitmq
```

In a separate terminal, start the deterministic eligibility provider:

```shell
cd infra/eligibility-worker
npx wrangler dev
```

Then export the application connection variables. Spring Boot does not load the
root `.env` file automatically when Maven starts the application, so replace
the two password placeholders with the values from that file.

On Windows PowerShell:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/voting"
$env:DATABASE_USERNAME = "voting"
$env:DATABASE_PASSWORD = "<POSTGRES_PASSWORD from .env>"
$env:SPRING_RABBITMQ_ADDRESSES = "localhost:5672"
$env:SPRING_RABBITMQ_USERNAME = "voting"
$env:SPRING_RABBITMQ_PASSWORD = "<RABBITMQ_PASSWORD from .env>"
$env:ELIGIBILITY_BASE_URL = "http://localhost:8787"
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```shell
export DATABASE_URL="jdbc:postgresql://localhost:5432/voting"
export DATABASE_USERNAME="voting"
export DATABASE_PASSWORD="<POSTGRES_PASSWORD from .env>"
export SPRING_RABBITMQ_ADDRESSES="localhost:5672"
export SPRING_RABBITMQ_USERNAME="voting"
export SPRING_RABBITMQ_PASSWORD="<RABBITMQ_PASSWORD from .env>"
export ELIGIBILITY_BASE_URL="http://localhost:8787"
./mvnw spring-boot:run
```

`ELIGIBILITY_BASE_URL` is required deliberately so startup fails instead of
silently using the discontinued challenge endpoint.

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
  "associateCpf": "19839091069",
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
