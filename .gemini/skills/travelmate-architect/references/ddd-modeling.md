# DDD Modeling Reference

## DDD Tactical Patterns
- **Aggregate Roots**: Java classes extending `AggregateRoot` (NOT records). Use factory methods for creation.
- **Value Objects**: Java Records with self-validation via `Assertion` in compact constructors.
- **Commands/Representations**: Java Records in `application/command/` or `application/representation/`.
- **Domain Events**: Java Records in `travelmate-common`. Registered via `registerEvent()` in aggregates.

## Aggregate Design
- Every aggregate is scoped by `TenantId` — no cross-tenant access.
- Aggregates enforce their own invariants.
- Application Services must NOT contain business logic.

## Event Flow
1. Command → Application Service → Aggregate method → `registerEvent(event)`
2. Application Service calls `repository.save(aggregate)`
3. `@TransactionalEventListener(phase = AFTER_COMMIT)` forwards to RabbitMQ
4. Consumer in other SCS processes event

## Bounded Contexts
- **IAM** (Supporting Subdomain)
- **Trips** (Core Subdomain)
- **Expense** (Generic Subdomain)
- **Gateway** (Anti-Corruption Layer / Entry point)
