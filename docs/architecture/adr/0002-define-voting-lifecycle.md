# ADR-0002: Define the Voting Lifecycle

- Status: Accepted
- Date: 2026-07-19

## Context

The challenge defines agenda creation, a time-bound voting session, one `YES` or `NO` vote per associate and agenda, and a final tally. It does not define repeated sessions, result visibility while voting, tie handling, exact time boundaries, or concurrent closure behavior.

These decisions must be explicit because they affect the API contract, persistence constraints, tests, and result publication.

## Decision

### Agenda and session

- An agenda has a title and an optional description.
- An agenda can have exactly one voting session.
- Opening a second session for the same agenda is rejected.
- A session opens immediately when it is created.
- The requested duration is expressed in seconds.
- The default duration is 60 seconds.
- The accepted duration range is 1 second through 24 hours.

### Associate identity and eligibility

- The associate CPF is the unique voting identifier.
- The API accepts an unformatted, valid 11-digit CPF.
- Syntactically invalid CPFs are rejected before the external eligibility call.
- An associate must be considered eligible by the configured external provider before a vote can be persisted.
- External provider unavailability is not interpreted as eligibility and produces a service-unavailable response.

### Vote acceptance

- Vote choices are `YES` and `NO`.
- A session accepts a vote while `openedAt <= now < closesAt`.
- The exact `closesAt` instant is closed.
- An associate can cast one vote per voting session.
- A PostgreSQL unique constraint is the final authority for duplicate-vote prevention.
- Eligibility is checked before opening the short database transaction used to persist a vote.

### Result

- Partial results are not exposed while the session is open.
- Once closed, the result contains `YES`, `NO`, and total counts.
- More `YES` votes produces `APPROVED`.
- More `NO` votes produces `REJECTED`.
- Equal counts, including zero votes, produces `TIED`.
- Closing persists an immutable result and a result-publication outbox event in the same transaction.
- Scheduled and on-demand closure use the same idempotent use case.

### Delivery guarantee

- RabbitMQ publication is at least once.
- Every event has a unique event identifier.
- Consumers are expected to deduplicate by event identifier.
- A broker outage does not roll back or lose a finalized voting result.

## HTTP Semantics

- Successful resource creation returns `201 Created`.
- Invalid syntax or values return `400 Bad Request`.
- Missing agendas return `404 Not Found`.
- State conflicts, such as a duplicate vote, duplicate session, closed session, or result requested while open, return `409 Conflict`.
- An ineligible or unknown associate returns `422 Unprocessable Content`.
- An unavailable required external dependency returns `503 Service Unavailable`.

Errors use RFC 9457 Problem Details and include a stable application error code.

## Consequences

- Session state is deterministic from persisted timestamps and does not depend on an in-memory timer.
- The database protects correctness when multiple requests race.
- Result publication survives application restarts.
- The public API deliberately avoids exposing intermediate vote counts.
