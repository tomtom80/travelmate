# AGENT.md — Development Rules for Travelmate

This document defines the binding rules and development process for all agents working on this project.

## 1. Development Process (iterative, DDD-driven)

The development process follows a fixed sequence. No phase may be skipped.

### 1.1 Strategic DDD (per iteration)
- Maintain the Context Map: Bounded Contexts and their relationships (U/D Conformist, Partnership, etc.)
- Respect subdomain classification: **Core** (Trips), **Supporting** (IAM), **Generic** (Expense)
- Follow the Ubiquitous Language: UI uses domain language (DE/EN via i18n), code uses technical names (see ADR-0011)

### 1.2 Tactical DDD (per Bounded Context)
- Aggregate Design: clearly delineate Aggregate Roots, Entities, and Value Objects
- Identify Domain Events (Event Storming as input)
- Define Repository interfaces as Ports in the domain — implementations belong in `adapters/persistence/`
- Application Services orchestrate use cases and must not contain business logic

### 1.3 Hexagonal Architecture (Ports & Adapters)
- **Domain layer is framework-free** — no Spring annotations, no JPA, no external dependencies
- Aggregate Roots: regular classes extending `AggregateRoot`
- Value Objects, Commands, Events, Representations: Java Records with self-validation via `Assertion`
- Adapter layer encapsulates infrastructure (Web, Persistence, Messaging)

## 2. TDD — Test-Driven Development

**Strictly Red-Green-Refactor.** No production code without a prior failing test.

### Test order (inside-out)
1. **Domain Unit Tests** — plain JUnit 5 + AssertJ, no Spring context
2. **Application Service Tests** — Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`)
3. **Persistence Adapter Tests** — `@SpringBootTest` + `@ActiveProfiles("test")` with H2
4. **Controller Tests** — `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@MockitoBean`
5. **E2E Tests** — Playwright (Java API) against running infrastructure

### Test rules
- Tests are NEVER added after the fact — always write the test first
- `final` on all local variables and parameters in tests
- Test profile: H2 in-memory, Flyway disabled, security permits all requests, RabbitMQ disabled (port 0)
- Every test must be independent and repeatable

## 3. Code Review (PR Workflow)

### Process
- Feature branch per User Story / Aggregate
- Create PR with summary + test plan
- Review by specialized agents (all must pass):

### Review Agents
| Agent | Checks |
|-------|--------|
| **code-reviewer** | Code style, project conventions, CLAUDE.md compliance |
| **type-design-analyzer** | Aggregate/VO design, invariants, encapsulation, DDD rules |
| **silent-failure-hunter** | Error handling, silent failures, missing validations |
| **pr-test-analyzer** | Test coverage, edge cases, missing test scenarios |
| **code-simplifier** | Simplification, clarity, unnecessary complexity |

## 4. Security Review

### Architecture Level
- Every SCS is an OAuth2 Resource Server — JWT validation is mandatory
- Gateway is the sole OIDC entry point (TokenRelay)
- Tenant isolation: every access must be scoped by `TenantId`
- Use Keycloak roles (`organizer`, `participant`) for authorization

### Code Level
- No SQL injection: use exclusively Spring Data JPA / parameterized queries
- No XSS: Thymeleaf escapes by default, no `th:utext` without review
- No command injection: no shell calls with user input
- CSRF protection active (Spring Security default)
- Input validation at system boundaries: Value Objects self-validate via `Assertion`
- SecurityConfig with `@Profile("!test")` / `@Profile("test")` — production security must never be accidentally disabled

### Dependency Level
- Keep dependencies up to date (Spring Boot BOM managed)
- Do not introduce unnecessary dependencies

## 5. QA / E2E Tests

- QA engineer designs test cases per User Story
- All use cases must be covered by E2E tests — not just the happy path
- Required scenarios: validation errors, permissions, negative tests, boundary values
- E2E tests run against the full Docker Compose infrastructure

## 6. Code Style (binding)

- **No Lombok** — Java Records for value types, regular classes for Aggregate Roots
- No wildcard imports — always use explicit imports
- Import order: static imports, then `java`, `jakarta`, `org`, `com`, others (each separated by a blank line)
- Always use braces for `if`, `for`, `while`, `do-while` (even single-line bodies)
- `final` on all local variables and parameters
- Member ordering: enums > static final fields (public>private) > static fields > static initializers > final fields > fields > instance initializers > constructors > static methods > methods > interfaces > static classes > classes

## 7. Documentation

- **ADRs**: New architecture decisions as MADR (German) in `docs/adr/`
- **Arc42**: Keep building blocks, runtime views, and deployment views up to date in `docs/arc42/`
- **i18n**: At least German and English, language switchable at runtime
- Code comments only where the logic is not self-explanatory

## 8. Architecture Invariants

These rules MUST NOT be violated:

1. Domain layer is framework-free
2. Every aggregate is isolated by `TenantId`
3. Communication between SCS is exclusively asynchronous (RabbitMQ)
4. Every SCS has its own database — no shared database
5. Flyway owns the schema (`ddl-auto=validate`)
6. UI is server-rendered (Thymeleaf + HTMX) — no SPA
7. Event publishing: first `save()`, then events via `@TransactionalEventListener(AFTER_COMMIT)`
8. Shared Kernel (`travelmate-common`) contains only domain primitives and event contracts — no business logic
