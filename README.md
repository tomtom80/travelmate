# Travelmate

Travelmate is a multi-tenant travel planning platform built as a Maven monorepo. It combines identity and access management, trip planning, and expense management in a Self-Contained Systems architecture with a server-rendered UI.

## Release

Current repository release state: `v0.12.3`

Patch release `v0.12.3` includes:

- demo hosting and operations documentation
- demo deployment automation via GitHub Actions and GHCR
- Hetzner/Brevo demo environment templates and bootstrap scripts
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

## Betriebsmodi

### Lokale Entwicklung

Die lokale Entwicklungsumgebung bleibt beim bestehenden Compose-Stack:

```bash
docker compose up -d
./mvnw clean verify
```

Merkmale:

- nutzt `docker-compose.yml`
- verwendet lokale PostgreSQL-, RabbitMQ-, Keycloak- und Mailpit-Container
- ist fuer Weiterentwicklung und lokale Tests gedacht

### Demo-Umgebung

Fuer eine oeffentlich erreichbare Demo gibt es einen getrennten Betriebsweg:

- Compose-Datei: [`docker-compose.demo.yml`](./docker-compose.demo.yml)
- Beispiel-Variablen: [`.env.demo.example`](./.env.demo.example)
- Reverse Proxy: [`Caddyfile.demo`](./Caddyfile.demo)
- automatischer Redeploy: [`scripts/deploy-demo.sh`](./scripts/deploy-demo.sh)
- Server-Bootstrap: [`scripts/bootstrap-demo-server.sh`](./scripts/bootstrap-demo-server.sh)
- konkrete Demo-Env-Vorlage: [`.env.demo.hetzner-brevo.example`](./.env.demo.hetzner-brevo.example)
- GitHub Actions CI: [`.github/workflows/ci.yml`](./.github/workflows/ci.yml)
- GitHub Actions Demo-Deploy: [`.github/workflows/demo-deploy.yml`](./.github/workflows/demo-deploy.yml)

Merkmale:

- verwendet echtes SMTP statt Mailpit
- ist fuer haeufige Redeployments per GitHub Actions ausgelegt
- laeuft typischerweise auf einer separaten Hetzner-VM
- ist bewusst vom lokalen Dev-Setup getrennt

Weiterfuehrende Betriebsdoku:

- [`docs/operations/2026-03-26-demo-hosting-empfehlung.md`](./docs/operations/2026-03-26-demo-hosting-empfehlung.md)
- [`docs/operations/2026-03-26-demo-betriebskonzept.md`](./docs/operations/2026-03-26-demo-betriebskonzept.md)
- [`docs/operations/2026-03-26-kubernetes-hosting-marktrecherche.md`](./docs/operations/2026-03-26-kubernetes-hosting-marktrecherche.md)

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
