# Travelmate

Travelmate is a multi-tenant travel planning platform built as a Maven monorepo. It combines identity and access management, trip planning, and expense management in a Self-Contained Systems architecture with a server-rendered UI.

## Release

Current repository release state: `v0.12.2`

Patch release `v0.12.2` includes:

- editable travel party names with cross-SCS event propagation
- trip-scoped recipes created directly in the trip context
- updated E2E and BDD coverage for the new recipe scoping
- refreshed project and release documentation

## Architecture Overview

Modules:

- `travelmate-common`: shared kernel for domain primitives and event contracts
- `travelmate-gateway`: Spring Cloud Gateway with OIDC login and token relay
- `travelmate-iam`: travel parties, accounts, dependents, registration
- `travelmate-trips`: trips, invitations, participants, recipes, meal plans, shopping lists
- `travelmate-expense`: party account, receipts, weightings, advance payments
- `travelmate-e2e`: Playwright and Cucumber based end-to-end tests

Technology:

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Thymeleaf + HTMX
- PostgreSQL + Flyway
- RabbitMQ
- Keycloak

## Quick Start

Prerequisites:

- Java 21
- Docker and Docker Compose

Start infrastructure:

```bash
docker compose up -d
```

Build the project:

```bash
./mvnw clean verify
```

Build and start all services with Compose:

```bash
docker compose up --build
```

## Tests

Run tests for a single module:

```bash
./mvnw -pl travelmate-trips clean test
```

Run the E2E suite:

```bash
./mvnw -Pe2e -pl travelmate-e2e clean verify -DskipTests=false
```

## Documentation

Technical and product documentation is available under [`docs/`](./docs/README.md):

- Architecture: `docs/arc42/`
- Decisions: `docs/adr/`
- Backlog and releases: `docs/backlog/`
- Design and UX: `docs/design/`
- Test cases: `docs/test-cases/`

## License

This project is licensed under the Apache License 2.0. See [`LICENSE`](./LICENSE) for details.
