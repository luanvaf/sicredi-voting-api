# Sicredi Voting API

A Java 21 and Spring Boot implementation of Sicredi's backend voting challenge.

The application manages agendas, time-bound voting sessions, unique associate votes, result calculation, associate eligibility checks, and reliable result publication through messaging.

## Status

Implementation in progress.

## Architecture

The project uses a pragmatic Java adaptation of Modular Agentic Semantic Architecture (MASA) while preserving Clean Architecture dependency inversion. See [ADR-0001](docs/architecture/adr/0001-adopt-masa-inspired-clean-architecture.md).

## Technology Baseline

- Java 21
- Spring Boot 4
- PostgreSQL
- RabbitMQ
- Flyway
- Testcontainers
- WireMock
- OpenAPI
- Docker

Complete execution, API, testing, performance, and deployment instructions will be added as the implementation progresses.
