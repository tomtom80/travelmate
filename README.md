# Travelmate

Travelmate helps families plan trips, manage participants, and keep shared travel expenses transparent.

## Current State

- Latest stable release: `v0.18.0`
- Working version on `main`: `0.19.0-SNAPSHOT`
- Current focus: CI and delivery hardening, documentation refresh, and production-readiness follow-ups

Delivered product scope today:

- travel party sign-up, member invitations, dependent management, and registration via invitation token
- trip CRUD, participant management, external email invitations, and trip-local multi-organizer support
- collaborative planning with date polls and accommodation polls, including booking confirmation flow
- recipes, meal plans, shopping lists, accommodation management, and accommodation URL import
- expense tracking with receipt capture, OCR-assisted scan flow, weightings, advance payments, and settlement PDF export

## Architecture Overview

Modules:

- `travelmate-common`: shared kernel for domain primitives and event contracts
- `travelmate-gateway`: Spring Cloud Gateway with OIDC login and token relay
- `travelmate-iam`: travel parties, accounts, dependents, registration
- `travelmate-trips`: trips, invitations, participants, recipes, meal plans, shopping lists, planning polls
- `travelmate-expense`: receipts, weightings, advance payments, settlements
- `travelmate-e2e`: Playwright and Cucumber based end-to-end tests (enabled via Maven profile)

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

Start local infrastructure:

```bash
docker compose up -d
```

Run the build:

```bash
./mvnw -B -ntp verify
```

Start the local stack:

```bash
docker compose up --build
```

Run the E2E suite:

```bash
./mvnw -Pe2e -pl travelmate-e2e verify
```

## Operating Modes

### Local Development

The local setup uses the standard Compose stack:

```bash
docker compose up -d
./mvnw -B -ntp verify
```

Characteristics:

- uses `docker-compose.yml`
- runs local PostgreSQL, RabbitMQ, Keycloak, and Mailpit containers
- is intended for development, integration tests, and E2E verification

### Demo Delivery

The public demo uses a separate delivery path:

- Compose file: [`docker-compose.demo.yml`](./docker-compose.demo.yml)
- example variables: [`.env.demo.example`](./.env.demo.example)
- reverse proxy: [`Caddyfile.demo`](./Caddyfile.demo)
- redeploy script: [`scripts/deploy-demo.sh`](./scripts/deploy-demo.sh)
- server bootstrap: [`scripts/bootstrap-demo-server.sh`](./scripts/bootstrap-demo-server.sh)
- GitHub Actions CI: [`.github/workflows/ci.yml`](./.github/workflows/ci.yml)
- GitHub Actions demo deploy: [`.github/workflows/demo-deploy.yml`](./.github/workflows/demo-deploy.yml)

Important GitHub and GHCR prerequisites:

- repository Actions workflow permissions must allow `Read and write`
- the workflow already requests `packages: write`, so `GITHUB_TOKEN` is sufficient only if the GHCR package is linked to this repository
- existing GHCR packages such as `ghcr.io/<owner>/travelmate-iam` must either be linked to `tomtom80/travelmate` or explicitly grant repository access under `Manage Actions access`

Further operations notes:

- [`docs/operations/2026-03-26-demo-hosting-empfehlung.md`](./docs/operations/2026-03-26-demo-hosting-empfehlung.md)
- [`docs/operations/2026-03-26-demo-betriebskonzept.md`](./docs/operations/2026-03-26-demo-betriebskonzept.md)
- [`docs/operations/2026-03-27-demo-go-live-checkliste.md`](./docs/operations/2026-03-27-demo-go-live-checkliste.md)
- [`docs/operations/2026-04-27-release-and-demo-delivery-status.md`](./docs/operations/2026-04-27-release-and-demo-delivery-status.md)

## Documentation

The main documentation lives under [`docs/`](./docs/README.md):

- architecture: `docs/arc42/`
- ADRs: `docs/adr/`
- backlog and releases: `docs/backlog/`
- design and UX: `docs/design/`
- operations: `docs/operations/`
- test cases: `docs/test-cases/`

## License

This project is licensed under the Apache License 2.0. See [`LICENSE`](./LICENSE) for details.
