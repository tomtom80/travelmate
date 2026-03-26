# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Travelmate is a multi-tenant travel management platform built as a **Maven multi-module monorepo** following the **Self-Contained Systems (SCS)** architecture. Each bounded context is an independent Spring Boot application with its own database, UI, and backend.

### Modules

| Module | Type | Port | Context Path | Description |
|--------|------|------|-------------|-------------|
| `travelmate-common` | Plain JAR (no Spring Boot) | — | — | Shared kernel: domain primitives, event contracts, RabbitMQ routing keys |
| `travelmate-gateway` | Spring Cloud Gateway (reactive) | 8080 | `/` | OIDC entry point, routes requests to SCS backends via TokenRelay |
| `travelmate-iam` | SCS (servlet) | 8081 | `/iam` | Identity & Access Management: sign-up, tenants, accounts, dependents, Keycloak integration |
| `travelmate-trips` | SCS (servlet) | 8082 | `/trips` | Trip management: travel parties, trips, invitations, stay periods |
| `travelmate-expense` | SCS (servlet) | 8083 | `/expense` | Expense management: ledgers, receipts, weightings |
| `travelmate-e2e` | Test module (profile `e2e`) | — | — | Playwright E2E tests against running infrastructure |

## Versioning

Maven CI-Friendly Versions (`${revision}` property). Current version: `0.12.2`.

Schema: `<major>.<iteration>.<patch>-SNAPSHOT`. The `<iteration>` tracks development iterations.

```bash
# Release workflow:
# 1. Set release version:  <revision>0.12.2</revision>
# 2. ./mvnw clean verify && git commit -am "Release v0.12.2" && git tag v0.12.2
# 3. Set next SNAPSHOT:    <revision>0.13.0-SNAPSHOT</revision>
# 4. git commit -am "chore: prepare 0.13.0-SNAPSHOT"
```

The `flatten-maven-plugin` resolves `${revision}` at build time. `.flattened-pom.xml` files are git-ignored.

## Build & Test Commands

All commands run from the **repository root** using the Maven Wrapper.

```bash
# Build entire project (compile + test + package)
./mvnw clean verify

# Run tests for a single module
./mvnw -pl travelmate-iam clean test

# Run a single test class
./mvnw -pl travelmate-iam test -Dtest=AccountTest

# Run a single test method
./mvnw -pl travelmate-iam test -Dtest=AccountTest#registerCreatesAccountWithEvent

# Build common + one SCS (when common changes affect an SCS)
./mvnw -pl travelmate-common,travelmate-iam clean test

# Run a single SCS locally (infrastructure must be running)
./mvnw -pl travelmate-iam spring-boot:run

# E2E tests (requires running docker compose infrastructure)
./mvnw -Pe2e verify
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
- Aggregate Roots are regular classes extending `AggregateRoot` (mutable event list); Value Objects, Commands, Events, and Representations are Java Records
- Value objects self-validate in compact constructors using `Assertion` utility from common
- `AggregateRoot` base class manages domain events (`registerEvent()`, `clearDomainEvents()`)
- Every aggregate is scoped by `TenantId`
- UI is server-rendered: Thymeleaf templates + HTMX (via CDN), not SPA
- SecurityConfig uses `@Profile("!test")` / `@Profile("test")` for dual security setup

### Shared Kernel (travelmate-common)

Contains domain primitives shared across all SCS:
- `TenantId`, `DomainEvent`, `AggregateRoot`, `Assertion` (validation utility)
- Event contracts in `events/iam/`: `AccountRegistered`, `MemberAddedToTenant`, `DependentAddedToTenant`, `DependentRemovedFromTenant`, `MemberRemovedFromTenant`, `TenantCreated`, `TenantDeleted`, `RoleAssignedToUser`, `RoleUnassignedFromUser`
- Event contracts in `events/trips/`: `TripCreated`, `ParticipantJoinedTrip`, `TripCompleted`
- `RoutingKeys` constants for RabbitMQ topic exchange (`travelmate.events`)

### Event Publishing Pattern

Domain events are registered in aggregate factory methods, then published after `repository.save()` via Spring `ApplicationEventPublisher`. A `@TransactionalEventListener(phase = AFTER_COMMIT)` in the messaging adapter forwards events to RabbitMQ. The messaging adapter uses `@Profile("!test")` to disable RabbitMQ in tests.

### Service Communication

Asynchronous via RabbitMQ (topic exchange `travelmate.events`):
- **IAM** publishes `TenantCreated`, `AccountRegistered`, `MemberAddedToTenant`, `DependentAddedToTenant` (and removal counterparts) events
- **Trips** consumes IAM events to maintain TravelParty projections; publishes `TripCreated`, `ParticipantJoinedTrip`, `TripCompleted`
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
- Spring Data JPA + PostgreSQL 16 + Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`)
- Thymeleaf + HTMX 2.0 + htmx-spring-boot-thymeleaf 5.0.0 (server-side rendering)
- Testing: JUnit 5, AssertJ, Mockito, Spring Boot Test, Spring Security Test, Spring Rabbit Test, H2
- E2E: Playwright 1.51.0 (Java API)

## Spring Boot 4.0 Specifics

These are non-obvious differences from Spring Boot 3.x:
- `@AutoConfigureMockMvc` moved to package `org.springframework.boot.webmvc.test.autoconfigure` — requires `spring-boot-starter-webmvc-test` test dependency
- `@MockBean` replaced by `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`
- Spring Cloud Gateway artifact: `spring-cloud-starter-gateway-server-webflux` (renamed)
- H2Dialect no longer needed in test profiles (Hibernate auto-detects)
- Flyway autoconfiguration moved to separate module — requires `spring-boot-starter-flyway` (not just `flyway-core`)

## Testing Conventions

- `@SpringBootTest` + `@ActiveProfiles("test")` for integration tests
- Test profile: H2 in-memory DB, Flyway disabled, security permits all requests, RabbitMQ disabled (port 0)
- Domain unit tests: plain JUnit 5 + AssertJ, no Spring context
- Application service tests: Mockito (`@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`)
- Persistence adapter tests: `@SpringBootTest` + `@ActiveProfiles("test")` with H2
- Controller tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@MockitoBean` for services
- `final` on all test locals and parameters

## Code Style Conventions

- **No Lombok** — Java Records for value types, regular classes for Aggregate Roots
- Never use wildcard imports — explicit imports only
- Import order: static imports, then `java`, `jakarta`, `org`, `com`, others (each separated by blank line)
- Always use braces for `if`, `for`, `while`, `do-while` (even single-line bodies)
- `final` on all locals and parameters
- Continuation indent: 4 spaces (same as regular indent)
- Member ordering: enums → static final fields (public→private) → static fields → static initializers → final fields → fields → instance initializers → constructors → static methods → methods → interfaces → static classes → classes

## Ubiquitous Language (ADR-0011)

The UI uses domain language (German/English via i18n), while code uses technical names:

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| Reisepartei | Travel Party | Tenant | IAM — registration unit (person/family) |
| Mitglied | Member | Account | IAM — person with login |
| Mitreisende(r) | Companion | Dependent | IAM — person without login |
| Reise | Trip | Trip | Trips — a concrete trip/event |
| Einladung | Invitation | Invitation | Trips — invitation to a trip (NOT IAM) |

Invitations belong to the Trips Core Domain, not IAM.

## Sign-up & Authentication Flow

1. Public sign-up page (`/iam/signup`) creates Tenant + Keycloak user + Account atomically via `SignUpService`
2. After sign-up, user is redirected to Gateway OIDC login
3. Gateway forwards JWT to SCS via TokenRelay filter
4. IAM resolves tenant context from JWT `email` claim via `AccountService`
5. Trips resolves `tenantId` + `memberId` from JWT `email` claim via `TravelParty` projection
6. Member invitation: organizer invites via email → invitee registers with existing Tenant

## Documentation

- 18 ADRs in `docs/adr/` (MADR format, German)
- Arc42 architecture docs in `docs/arc42/`
- Design diagrams in `docs/design/`
