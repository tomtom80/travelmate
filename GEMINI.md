# Travelmate Project — Architectural Mandates

This file defines the foundational mandates and engineering standards for the Travelmate project. These rules take absolute precedence over general workflows.

## 🏗️ Core Architecture (Self-Contained Systems & SCS)
- **Monorepo Structure**: Maven multi-module. Modules: `common` (Shared Kernel), `gateway` (Entry), `iam`, `trips`, `expense` (SCS).
- **Tech Stack**: Java 21, Spring Boot 4.x, RabbitMQ, PostgreSQL, Keycloak, Thymeleaf + HTMX.
- **Hexagonal Architecture (Ports & Adapters)**:
    - **Domain Layer (`domain/`)**: ZERO framework imports (no Spring, JPA, Jakarta). Repository interfaces are PORTS.
    - **Adapters Layer (`adapters/`)**: Implementations for persistence, web (`adapters/web/`), and messaging (`adapters/messaging/`).
    - **Application Layer (`application/`)**: Orchestrates use cases; no business logic here.

## 🧱 DDD Tactical Patterns
- **Aggregate Roots**: Java classes extending `AggregateRoot` (NOT records).
- **Value Objects**: Java Records with self-validation via `Assertion` in compact constructors.
- **Commands/Representations**: Java Records in `application/command/` or `application/representation/`.
- **Domain Events**: Java Records in `travelmate-common`. Registered via `registerEvent()` in aggregates.
- **Tenant Isolation**: EVERY aggregate and query MUST be scoped by `TenantId`. Cross-tenant access is a security vulnerability.

## 🔄 Event Flow & Integration
1. **Command** → **Application Service** → **Aggregate method** → `registerEvent(event)`.
2. **Service** calls `repository.save(aggregate)`.
3. `@TransactionalEventListener(phase = AFTER_COMMIT)` forwards to RabbitMQ (`travelmate.events`).
4. Communication between SCS is exclusively asynchronous via RabbitMQ.

## 🔐 Security Standards
- **OAuth2/OIDC**: Gateway is the sole entry point (TokenRelay). Every SCS is a Resource Server.
- **Authorization**: Use Keycloak roles (`organizer`, `participant`). Resolve `TenantId` from JWT email claim.
- **Injection Prevention**: No string concatenation in queries. Use Spring Data JPA. No `nativeQuery=true` with user input.
- **Validation**: Strict self-validation in Value Objects. No `th:utext` without explicit approval.
- **Dangerous Patterns**: Block/Warn on `rm -rf /`, `DROP DATABASE`, `TRUNCATE`, `--no-verify`, `force-push` in shell commands.
- **Sensitive Files**: Log/Notify on modifications to `SecurityConfig`, `application.yml`, `docker-compose.yml`, `.env`.

## 📈 Current Project State (Iteration 10)
- **ADRs**: 18 ADRs exist. ADR-0016 (SSRF Protection), ADR-0017 (OCR choice), ADR-0018 (Local LLM for Import).
- **Architecture Risks**:
    - Expense uses `ParticipantWeighting` + `PartySettlement` aggregation.
    - SSRF risk in URL fetching (ADR-0016).
    - OCR (Tesseract) requires native dependencies in Docker.
- **EventStorming**: Sessions documented for Expense (Iter 6), Trips (Iter 8), Iteration 9 & 10.

## 📖 Documentation & Language
- **Ubiquitous Language**: Use technical names in code (Tenant, Account, Trip, Invitation); UI uses i18n (Reisepartei, Mitglied, Reise, Einladung).
- **ADRs**: MADR format in `docs/adr/`.
- **arc42**: Maintain documentation in `docs/arc42/`.

## 🧪 Testing Strategy
- **Unit Tests**: Domain logic verification without frameworks.
- **ArchUnit**: Enforce layer boundaries and naming conventions.
- **E2E**: Playwright tests for cross-SCS journeys.
