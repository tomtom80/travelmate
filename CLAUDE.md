# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Travelmate is a multi-tenant travel management platform built as a **Maven multi-module monorepo** following the **Self-Contained Systems (SCS)** architecture. Each bounded context is an independent Spring Boot application with its own database, UI, and backend.

### Modules

| Module | Type | Port | Context Path | Description |
|--------|------|------|-------------|-------------|
| `travelmate-common` | Plain JAR (no Spring Boot) | — | — | Shared kernel: domain primitives, event contracts, RabbitMQ routing keys |
| `travelmate-gateway` | Spring Cloud Gateway (reactive) | 8080 | `/` | OIDC entry point, routes requests to SCS backends via TokenRelay |
| `travelmate-iam` | SCS (servlet) | 8081 | `/iam` | Identity & Access Management: tenants, accounts, dependents |
| `travelmate-trips` | SCS (servlet) | 8082 | `/trips` | Trip management: travel parties, trips, invitations, polls |
| `travelmate-expense` | SCS (servlet) | 8083 | `/expense` | Expense management: ledgers, receipts, weightings |

## Versioning

Maven CI-Friendly Versions (`${revision}` property). Current version: `0.1.0-SNAPSHOT`.

Schema: `<major>.<iteration>.<patch>-SNAPSHOT`. The `<iteration>` tracks development iterations.

```bash
# Release workflow:
# 1. Set release version:  <revision>0.1.0</revision>
# 2. ./mvnw clean verify && git commit -am "release: v0.1.0" && git tag v0.1.0
# 3. Set next SNAPSHOT:    <revision>0.2.0-SNAPSHOT</revision>
# 4. git commit -am "chore: prepare 0.2.0-SNAPSHOT"
```

The `flatten-maven-plugin` resolves `${revision}` at build time. `.flattened-pom.xml` files are git-ignored.

## Build & Test Commands

All commands run from the **repository root** using the Maven Wrapper.

```bash
# Build entire project
./mvnw clean package

# Run tests for all modules
./mvnw clean test

# Run tests for a single module
./mvnw -pl travelmate-iam clean test

# Run a single test class
./mvnw -pl travelmate-iam test -Dtest=UserTest

# Run a single test method
./mvnw -pl travelmate-iam test -Dtest=UserTest#testMethodName

# Run a single SCS locally (infrastructure must be running)
./mvnw -pl travelmate-iam spring-boot:run
```

### Docker Infrastructure

```bash
# Start all infrastructure (PostgreSQL x4, RabbitMQ, Keycloak)
docker compose up -d

# Start everything including application services
docker compose up --build
```

| Infrastructure | Port | Credentials |
|---------------|------|-------------|
| PostgreSQL (IAM) | 5432 | travelmate / travelmate |
| PostgreSQL (Trips) | 5433 | travelmate / travelmate |
| PostgreSQL (Expense) | 5434 | travelmate / travelmate |
| PostgreSQL (Keycloak) | 5435 | keycloak / keycloak |
| Keycloak | 7082 | admin / admin |
| RabbitMQ (AMQP) | 5672 | guest / guest |
| RabbitMQ Management UI | 15672 | guest / guest |

Test users in Keycloak realm `travelmate`: `testuser` / `testpassword`, `admin` / `admin` (both have organizer + participant roles).

## Architecture

All SCS follow **DDD + Hexagonal Architecture (Ports & Adapters)**:

```
de.evia.travelmate.<context>/
├── domain/          # Pure business logic — NO Spring/framework dependencies
│   ├── <aggregate>/ # Entities, Value Objects, Repository interfaces (ports)
│   └── ...
├── application/     # Use cases: Application Services + Commands + Representations
└── adapters/
    ├── web/         # Controllers (Thymeleaf + HTMX), SecurityConfig
    ├── persistence/ # JPA Repository implementations
    └── messaging/   # RabbitMQ producers/consumers
```

**Key architectural rules:**
- Domain layer must remain free of framework dependencies
- Repository interfaces are defined in the domain, implementations live in adapters/persistence
- Java Records for entities, value objects, commands, and domain events (no Lombok)
- Value objects self-validate in compact constructors using `Assertion` utility from common
- `AggregateRoot` base class manages domain events (`registerEvent()`, `clearDomainEvents()`)
- Every aggregate is scoped by `TenantId`
- UI is server-rendered: Thymeleaf templates + HTMX (via CDN), not SPA
- SecurityConfig uses `@Profile("!test")` / `@Profile("test")` for dual security setup

### Shared Kernel (travelmate-common)

Contains domain primitives shared across all SCS:
- `TenantId`, `DomainEvent`, `AggregateRoot`, `Assertion` (validation utility)
- Event contracts: `AccountRegistered`, `MemberAddedToTenant`, `DependentAddedToTenant`, `TripCreated`, `ParticipantConfirmed`, `TripCompleted`
- `RoutingKeys` constants for RabbitMQ topic exchange (`travelmate.events`)

### Service Communication

Asynchronous via RabbitMQ (topic exchange `travelmate.events`):
- **IAM** publishes `AccountRegistered`, `MemberAddedToTenant`, `DependentAddedToTenant` events
- **Trips** consumes IAM events to maintain TravelParty projections; publishes `TripCreated`, `ParticipantConfirmed`, `TripCompleted`
- **Expense** consumes Trips events

### Authentication

- Keycloak 26.1.4 as OIDC provider (realm: `travelmate`, client: `travelmate-gateway`)
- Gateway handles OAuth2 login and forwards JWT to SCS via TokenRelay filter
- Gateway route config prefix: `spring.cloud.gateway.server.webflux.routes`
- SCS validate JWTs as OAuth2 Resource Servers
- Keycloak roles: `organizer`, `participant`

### Database

Each SCS has its own PostgreSQL database with Flyway migrations in `src/main/resources/db/migration/`. JPA ddl-auto is `validate` (Flyway owns the schema). Environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

## Tech Stack

- Java 21, Spring Boot 4.0.3, Spring Cloud 2025.1.1 (Oakwood), Maven (CI-Friendly Versions)
- Spring Cloud Gateway (reactive, OIDC)
- Spring Security OAuth2 Resource Server (JWT)
- Spring AMQP / RabbitMQ 4.0
- Spring Data JPA + PostgreSQL 16 + Flyway (with flyway-database-postgresql)
- Thymeleaf + HTMX 2.0 + htmx-spring-boot-thymeleaf 5.0.0 (server-side rendering)
- Testing: JUnit 5, AssertJ, Spring Boot Test, Spring Security Test, Spring Rabbit Test, H2

## Testing Conventions

- `@SpringBootTest` + `@ActiveProfiles("test")` for integration tests
- Test profile: H2 in-memory DB, Flyway disabled, security permits all requests, RabbitMQ disabled (port 0)
- Domain unit tests: plain JUnit 5 + AssertJ, no Spring context
- `final` on all test locals and parameters

## Code Style Conventions

- **No Lombok** — Java Records everywhere
- Never use wildcard imports — explicit imports only
- Import order: static imports, then `java`, `jakarta`, `org`, `com`, others (each separated by blank line)
- Always use braces for `if`, `for`, `while`, `do-while` (even single-line bodies)
- `final` on all locals and parameters
- Continuation indent: 4 spaces (same as regular indent)
- Member ordering: enums → static final fields (public→private) → static fields → static initializers → final fields → fields → instance initializers → constructors → static methods → methods → interfaces → static classes → classes

## Documentation

- 9 ADRs in `docs/adr/` (MADR format, German)
- Arc42 architecture docs in `docs/arc42/`
- Design diagrams in `docs/design/`
