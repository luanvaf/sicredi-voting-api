# Performance Tests

The k6 scenario in [`k6/voting-load-test.js`](k6/voting-load-test.js) creates a
new agenda, opens a 24-hour voting session, and submits an exact number of
unique votes. The standard run submits 100,000 votes with 200 virtual users.
Each run leaves its agenda, session, and votes in the configured database.

## Safety

**Do not point this test at production, the public recruiter demo, or any
environment you do not own without explicit authorization.** The standard run
creates 100,000 database records and makes 100,000 calls to the configured
eligibility provider. It can exhaust free-tier quotas, trigger rate limits, or
degrade a shared environment.

Start with smoke mode. Use an isolated database and an isolated eligibility
service for the full run. The session remains open for 24 hours, so do not
reuse a performance-test environment as a functional-test environment.

## Prerequisites

- The voting API and its PostgreSQL and RabbitMQ dependencies are already
  running.
- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) is installed.
- The API is configured with one of these eligibility arrangements:
  - Worker mode: point the API at the deterministic Worker under
    `infra/eligibility-worker` and leave k6's `ELIGIBILITY_MODE=worker`
    default. The scenario generates valid CPFs with an even final digit,
    matching the Worker's `ABLE_TO_VOTE` rule.
  - Always-eligible mode: point the API at a test stub that returns
    `200 {"status":"ABLE_TO_VOTE"}` for every valid CPF and run k6 with
    `ELIGIBILITY_MODE=always`. This setting changes CPF selection in k6; it
    does not replace or bypass the API's eligibility call.

The API's default external eligibility URL is not suitable for this test.
Configure `ELIGIBILITY_BASE_URL` before starting the API. For example, when the
API and Worker both run on the host:

```powershell
$env:ELIGIBILITY_BASE_URL = "http://localhost:8787"
.\mvnw.cmd spring-boot:run
```

Start the local Worker in a separate terminal:

```powershell
Set-Location infra/eligibility-worker
npx wrangler dev
```

When the API runs in Docker and the Worker runs on the host, use
`http://host.docker.internal:8787` as the API's eligibility base URL.

## Smoke Test

Smoke mode defaults to 10 votes, one virtual user, and a one-minute maximum:

```powershell
k6 run -e SMOKE=true -e BASE_URL=http://localhost:8080 performance/k6/voting-load-test.js
```

Run this first to verify connectivity, the eligibility configuration, and all
three `201 Created` contracts.

## 100,000-Vote Test

The standard configuration is explicit in this command:

```powershell
k6 run -e BASE_URL=http://localhost:8080 -e TOTAL_VOTES=100000 -e VUS=200 -e MAX_DURATION=15m -e ELIGIBILITY_MODE=worker performance/k6/voting-load-test.js
```

The scenario uses k6's `shared-iterations` executor. Its
`iterationInTest` value assigns every request a distinct deterministic CPF,
even when iterations move between virtual users. YES and NO votes alternate by
default.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | Voting API base URL |
| `SMOKE` | `false` | Uses lightweight defaults when set to `true` |
| `TOTAL_VOTES` | `100000` (`10` in smoke mode) | Exact number of vote requests |
| `VUS` | `200` (`1` in smoke mode) | Concurrent virtual users |
| `MAX_DURATION` | `15m` (`1m` in smoke mode) | Maximum scenario duration |
| `ELIGIBILITY_MODE` | `worker` | `worker` for even-final-digit CPFs, or `always` for an always-eligible stub |
| `VOTE_CHOICE` | `ALTERNATE` | `YES`, `NO`, or alternating choices |

Explicit variables override smoke defaults. Keep `MAX_DURATION` below the
24-hour session lifetime.

## Acceptance Thresholds

The run fails when:

- fewer than 99% of checks pass;
- 1% or more HTTP requests or vote requests fail;
- vote latency reaches 1 second at p95 or 2 seconds at p99.

Setup aborts immediately unless agenda creation and session creation both
return `201`. Every vote response is checked for `201`.
