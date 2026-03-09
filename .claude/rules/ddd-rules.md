# DDD & Architecture Rules (Always Active)

These rules are ALWAYS enforced when writing or reviewing code in the Travelmate project.

## Hexagonal Architecture
- Domain layer (`domain/`) must have ZERO framework imports (no `org.springframework`, no `jakarta.persistence`, no `jakarta.transaction`)
- Repository interfaces are PORTS defined in `domain/` — implementations are ADAPTERS in `adapters/persistence/`
- Controllers live in `adapters/web/`, messaging in `adapters/messaging/`

## DDD Tactical Patterns
- Aggregate Roots: regular Java classes extending `AggregateRoot` — NOT records
- Value Objects: Java Records with self-validation via `Assertion` in compact constructors
- Commands: Java Records in `application/command/`
- Events: Java Records implementing `DomainEvent` in `travelmate-common`
- Representations: Java Records in `application/representation/`
- Domain Events registered via `registerEvent()` in aggregate methods

## Aggregate Design
- Every aggregate is scoped by `TenantId` — no cross-tenant access
- Aggregates enforce their own invariants — Application Services must NOT contain business logic
- Factory methods on aggregates for creation (return aggregate + register creation event)

## Event Flow
1. Command → Application Service → Aggregate method → `registerEvent(event)`
2. Application Service calls `repository.save(aggregate)`
3. `@TransactionalEventListener(phase = AFTER_COMMIT)` forwards to RabbitMQ
4. Consumer in other SCS processes event

## Ubiquitous Language (ADR-0011)
- Code uses technical names: Tenant, Account, Dependent, Trip, Invitation
- UI uses domain language via i18n: Reisepartei, Mitglied, Mitreisende(r), Reise, Einladung
