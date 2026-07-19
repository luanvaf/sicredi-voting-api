# Deployment

This guide covers a local Docker Compose environment and a low-cost hosted
topology:

- Render builds and runs the API from its Dockerfile.
- Neon provides PostgreSQL.
- CloudAMQP provides an AMQP 0-9-1 broker.
- Cloudflare Workers hosts the associate eligibility substitute.

The committed configuration contains variable names and local service defaults,
but no credentials. Keep the Git repository private, keep `.env` local, and
store all hosted credentials in the Render dashboard.

## Free-tier expectations

The free plans are appropriate for evaluation, demonstrations, and small hobby
workloads. They are not an always-on production platform:

- A Render Free web service spins down after 15 minutes without inbound traffic
  and can take about a minute to start again.
- A Neon Free compute scales to zero when idle and has usage and storage limits.
- CloudAMQP's free shared broker has connection, queue, and message quotas.

Review the current provider limits before deploying. Use paid plans with
backups, availability guarantees, monitoring, and capacity sized for the
workload before treating this topology as production.

## Run locally with Docker Compose

Requirements:

- Docker Engine or Docker Desktop
- Docker Compose v2

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Set unique local values for `POSTGRES_PASSWORD` and `RABBITMQ_PASSWORD` in
`.env`. Do not reuse hosted credentials and do not commit the populated file.
The repository's `.gitignore` excludes `.env`.

Build and start the API, PostgreSQL, and RabbitMQ:

```shell
docker compose up --build --detach
docker compose ps
```

Compose waits for PostgreSQL and RabbitMQ health checks before starting the API.
The API image also reports readiness through
`/actuator/health/readiness`. All published ports bind only to
`127.0.0.1`.

Local endpoints:

- API health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- RabbitMQ management: `http://localhost:15672`

Use the RabbitMQ username and password from the local `.env` file for the
management UI.

Inspect logs or stop the environment:

```shell
docker compose logs --follow app
docker compose down
```

`docker compose down --volumes` also deletes the local PostgreSQL and RabbitMQ
data volumes. Use it only when that data can be discarded.

## Deploy with Render, Neon, and CloudAMQP

The Blueprint intentionally omits a repository URL. Render uses the private
repository that contains `render.yaml`; the repository does not need to be
public. Grant the Render Git integration access to this repository only, or to
the smallest practical repository set.

The Blueprint targets Render's Virginia region. Select US East (N. Virginia), or
the nearest available region, for Neon and CloudAMQP to reduce latency. Change
the Blueprint region before the first deployment if another common region is
more appropriate, because Render does not allow changing a service's region
after creation.

### 1. Deploy the eligibility Worker

The original eligibility endpoint supplied with the challenge is no longer
available. This repository includes a deterministic substitute under
`infra/eligibility-worker`.

1. Create or sign in to a Cloudflare account.
2. From `infra/eligibility-worker`, run `npx wrangler login`.
3. Run `npx wrangler deploy`.
4. Verify `https://<worker-name>.<account>.workers.dev/health`.
5. Store the Worker origin, without `/users/...`, as `ELIGIBILITY_BASE_URL` in
   Render.

The Worker validates CPF check digits. Its deterministic able/unable decision is
documented in the Worker README and exists only to make the discontinued
challenge dependency reproducible.

### 2. Create the Neon database

1. Create a Neon project and database.
2. Open the database connection details.
3. Select the **direct** connection, not the pooled endpoint. Flyway runs when
   the API starts, and Neon recommends direct connections for schema migration
   tools.
4. Record the host, database name, role, and password in a password manager.

Prepare these Render values:

```text
DATABASE_URL=jdbc:postgresql://<direct-host>/<database>?sslmode=require
DATABASE_USERNAME=<role>
DATABASE_PASSWORD=<password>
```

Do not include the username or password in `DATABASE_URL`. The Blueprint limits
HikariCP to five connections with one minimum idle connection, which is a
conservative starting point for the free database. If migrations are later
moved to a separate deployment step, the runtime datasource can be evaluated
for Neon's pooled endpoint.

### 3. Create the CloudAMQP broker

1. Create a CloudAMQP instance in the closest available region.
2. For a no-cost evaluation, select the current free shared plan. At the time of
   writing this is the Loyal Lemming LavinMQ plan. LavinMQ implements AMQP
   0-9-1, which is the protocol used by Spring AMQP.
3. In the instance overview, copy the complete AMQP URL, including its virtual
   host and TLS scheme.
4. Store that value as `RABBITMQ_URL` in Render.

Treat the AMQP URL as a secret because it contains credentials. Do not paste it
into source files, issues, screenshots, or deployment logs.

### 4. Create the Render Blueprint

1. Connect the private Git repository to Render.
2. In the Render dashboard, create a new Blueprint and select this repository.
3. Confirm that Render detects the root `render.yaml`.
4. During the initial Blueprint creation, supply these `sync: false` values:
   `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `RABBITMQ_URL`,
   and `ELIGIBILITY_BASE_URL`.
5. Create the Blueprint and follow the first deployment in the Render Events and
   Logs views.

Render supplies `PORT`; the Spring configuration binds to it automatically.
Render builds the multi-stage Java 21 image, runs it as a non-root user, and
checks `/actuator/health/readiness`. Flyway applies pending migrations during
application startup.

Variables marked `sync: false` are prompted only when a Blueprint creates a
service. Add or rotate them directly in the Render service environment for an
existing service, then redeploy.

### 5. Verify the deployment

After Render reports the deployment as live:

1. Open `https://<service>.onrender.com/actuator/health` and confirm an `UP`
   response.
2. Open `https://<service>.onrender.com/swagger-ui.html`.
3. Review the startup logs for successful Flyway migration, PostgreSQL
   connectivity, and AMQP connectivity.
4. Confirm that no credentials or complete connection URLs appear in logs.
5. Exercise the API and confirm that the expected exchange and queue appear in
   the CloudAMQP management console.

The first request after an idle period can be slow because both Render and Neon
may need to wake up. Do not use synthetic keep-alive traffic to evade free-tier
sleep behavior; upgrade the service when always-on availability is required.

## Credential rotation

Rotate Neon and CloudAMQP credentials in their provider dashboards, update the
matching Render environment variables, and redeploy the service. Revoke the old
credentials only after the new deployment is healthy. Never place temporary or
old credentials in Git.

## Provider references

- [Render Blueprints](https://render.com/docs/infrastructure-as-code)
- [Render free instance limits](https://render.com/docs/free)
- [Render health checks](https://render.com/docs/health-checks)
- [Neon connection pooling and migration guidance](https://neon.com/docs/connect/connection-pooling)
- [CloudAMQP getting started](https://www.cloudamqp.com/docs/index.html)
- [CloudAMQP plans](https://www.cloudamqp.com/plans.html)
- [LavinMQ AMQP 0-9-1](https://lavinmq.com/documentation/amqp-the-protocol)
