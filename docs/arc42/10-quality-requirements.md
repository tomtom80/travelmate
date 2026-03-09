# 10. Quality Requirements

This section documents the results of a Quality Storming session (INNOQ methodology) applied to the Travelmate platform. It covers quality attributes, concrete scenarios, priority trade-offs, observable metrics, and an evolution roadmap.

---

## 1. Quality Tree

```
Quality
├── Functional Suitability
│   ├── Functional Completeness (trip lifecycle, expense settlement)
│   ├── Functional Correctness (expense calculation, weighted splits)
│   └── Domain Integrity (aggregate invariants, value object validation)
│
├── Usability
│   ├── Mobile-First (responsive, touch-optimized)
│   ├── Ease of Use (minimal click paths, intuitive forms)
│   ├── Offline Capability (Service Worker, cached views)
│   └── Accessibility (semantic HTML via PicoCSS, keyboard navigation)
│
├── Reliability
│   ├── Data Consistency (eventual consistency via idempotent events)
│   ├── Event Delivery Guarantee (at-least-once, Dead Letter Queue)
│   ├── Fault Tolerance (SCS isolation, independent failure domains)
│   └── Recoverability (Flyway migrations, DB backups)
│
├── Performance Efficiency
│   ├── Response Time (server-rendered pages, HTMX partial updates)
│   ├── Resource Utilization (per-SCS resource budgets)
│   └── Scalability (horizontal SCS scaling, independent DBs)
│
├── Security
│   ├── Tenant Isolation (TenantId scoping on all aggregates)
│   ├── Authentication (OIDC via Keycloak, JWT validation)
│   ├── Authorization (role-based: organizer, participant)
│   ├── Input Validation (Value Object self-validation via Assertion)
│   └── Data Protection (GDPR: personal data in IAM only)
│
├── Maintainability
│   ├── Modularity (SCS architecture, independent deployables)
│   ├── Hexagonal Architecture (framework-free domain layer)
│   ├── Testability (TDD, 185+ tests, domain-first testing)
│   ├── Analyzability (clear package structure, DDD naming)
│   └── Architectural Conformance (invariants enforced by convention)
│
├── Compatibility
│   ├── Browser Compatibility (modern browsers, PWA support)
│   └── Interoperability (RabbitMQ event contracts in shared kernel)
│
└── Portability
    ├── Container Deployment (Docker Compose, future Kubernetes)
    ├── Installability (PWA: Add to Home Screen)
    └── Environment Adaptability (profile-based config: dev, test, prod)
```

---

## 2. Quality Scenarios

### 2.1 Functional Suitability

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-F01 | **Domain Integrity** | A Value Object is created with invalid data (e.g., empty TenantName, negative amount) | Any SCS, any environment | The compact constructor throws `IllegalArgumentException` via `Assertion` utility | 100% of Value Objects enforce invariants; zero invalid aggregates persisted |
| QS-F02 | **Functional Correctness** | An organizer creates a trip with overlapping stay periods for the same participant | Trips SCS, normal operation | The system rejects the overlapping period at the domain level | Domain unit test verifies rejection; no overlapping periods in DB |
| QS-F03 | **Event Contract Stability** | A new field is added to an IAM event (e.g., `AccountRegistered`) | Cross-SCS communication | Consumers ignore unknown fields (forward-compatible JSON deserialization) | Existing consumers continue to work without code changes |

### 2.2 Usability

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-U01 | **Mobile-First** | A participant views the trip details on a smartphone (360px viewport) | Any modern mobile browser | All content is readable without horizontal scrolling; touch targets are >= 44px | Lighthouse Mobile score >= 90; no horizontal overflow detected |
| QS-U02 | **Ease of Use** | A non-technical family member wants to accept a trip invitation | Production, first-time user | The user completes the flow (open link, sign up/log in, accept) in <= 3 steps after authentication | Usability test: 90% of test users complete within 3 minutes |
| QS-U03 | **Offline Capability** | A user opens the shopping list while in a cabin with no internet | PWA installed, data previously loaded | The cached shopping list is displayed from Service Worker cache | Service Worker serves cached response within 200ms |
| QS-U04 | **Accessibility** | A user navigates the application using only a keyboard | Desktop browser, no mouse | All interactive elements are reachable via Tab; focus indicators are visible | Lighthouse Accessibility score >= 90; WCAG 2.1 Level AA for navigation |

### 2.3 Reliability

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-R01 | **Event Idempotency** | RabbitMQ redelivers an `AccountRegistered` event due to consumer restart | Trips SCS consuming IAM events | The consumer handles the duplicate without creating a second TravelParty entry | No `DataIntegrityViolationException` propagated; exactly one TravelParty per account |
| QS-R02 | **Fault Isolation** | The Trips SCS is down for maintenance | IAM and Expense SCS running | IAM continues to function normally; events queue in RabbitMQ for later consumption | IAM response times unaffected; queued messages delivered when Trips recovers |
| QS-R03 | **Dead Letter Handling** | A malformed event cannot be deserialized by a consumer | Any SCS consuming events | The message is routed to a Dead Letter Queue after max retries | DLQ message count is alerted on; no message silently dropped |
| QS-R04 | **Database Recovery** | A PostgreSQL instance restarts unexpectedly | Single SCS affected | The SCS reconnects via HikariCP connection pool retry; Flyway validates schema on startup | SCS recovers within 30 seconds; no data loss |

### 2.4 Performance Efficiency

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-P01 | **Page Load Time** | A user navigates to the trip dashboard | Normal operation, < 50 concurrent users | The server renders and returns the full HTML page | Time to First Byte (TTFB) < 300ms; full page load < 1s |
| QS-P02 | **HTMX Partial Update** | A user adds a participant to a trip via HTMX | Normal operation | Only the participant list fragment is re-rendered and swapped | HTMX response < 200ms; no full page reload |
| QS-P03 | **Event Processing Latency** | IAM publishes a `MemberAddedToTenant` event | All SCS running, normal load | Trips SCS processes the event and updates TravelParty | End-to-end event latency < 2 seconds (publish to persistence) |
| QS-P04 | **Database Query Performance** | A trip dashboard loads all trips for a tenant | Tenant with up to 20 trips, 50 participants total | Queries use TenantId index; no N+1 queries | Dashboard query completes in < 100ms; max 3 SQL queries per page |

### 2.5 Security

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-S01 | **Tenant Isolation** | User A (Tenant 1) attempts to access a trip belonging to Tenant 2 by manipulating the URL | Any SCS, authenticated user | The system returns 404 (not 403) to avoid information leakage | Zero cross-tenant data access in security tests; every repository method filters by TenantId |
| QS-S02 | **JWT Validation** | A request arrives at an SCS with an expired or tampered JWT | Any SCS as Resource Server | The SCS rejects the request with HTTP 401 | 100% of requests without valid JWT are rejected |
| QS-S03 | **Role Enforcement** | A participant (non-organizer) attempts to create a trip | Trips SCS, authenticated user with `participant` role only | The system returns HTTP 403 Forbidden | Controller test verifies 403 for non-organizer; Spring Security `@PreAuthorize` enforced |
| QS-S04 | **Input Sanitization** | A user submits a trip name containing `<script>alert('xss')</script>` | Any SCS, form submission | Thymeleaf auto-escapes the output; no script execution in rendered HTML | No `th:utext` usage without explicit review; XSS test passes |
| QS-S05 | **CSRF Protection** | An attacker crafts a cross-site POST request to delete a tenant | Any SCS, user has active session | Spring Security CSRF token validation rejects the request | CSRF token required on all state-changing operations |

### 2.6 Maintainability

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-M01 | **Framework-Free Domain** | A developer adds JPA annotations to a domain entity | Development, code review | The build or review process rejects the change | ArchUnit test: no `jakarta.persistence` or `org.springframework` imports in `domain` packages |
| QS-M02 | **Independent Deployability** | A developer ships a new feature in Trips SCS | Production deployment | Trips is deployed independently without restarting IAM or Expense | Deployment pipeline deploys single SCS; other SCS health checks remain green |
| QS-M03 | **Test Coverage** | A developer adds a new aggregate | Development, TDD | The aggregate has domain unit tests, application service tests, and persistence adapter tests | >= 80% line coverage per module; every aggregate has tests at all 3 layers |
| QS-M04 | **Onboarding Time** | A new developer joins the project | Development team | The developer can build, run tests, and start the system locally | Full `docker compose up -d && ./mvnw clean verify` succeeds within 10 minutes |
| QS-M05 | **Architectural Conformance** | A developer introduces a direct database call from the domain layer | Development, CI pipeline | The violation is detected automatically | ArchUnit tests enforce hexagonal layer rules; CI fails on violation |

### 2.7 Portability

| ID | Quality Attribute | Stimulus | Environment | Response | Response Measure |
|----|-------------------|----------|-------------|----------|------------------|
| QS-PO1 | **Container Deployment** | The system is deployed to a new environment (staging) | Docker Compose or Kubernetes | All SCS start successfully with environment-specific configuration | Health endpoints return 200 within 60 seconds of container start |
| QS-PO2 | **Database Portability** | Tests run against H2 instead of PostgreSQL | Test profile, CI environment | All repository tests pass on H2 without modification | Zero test failures due to DB dialect differences |

---

## 3. Priority Matrix

Scenarios are rated using **MoSCoW priority** (Must/Should/Could/Won't) crossed with **Risk level** (High/Medium/Low).

| Scenario | Priority | Risk | Rationale |
|----------|----------|------|-----------|
| QS-S01 Tenant Isolation | **Must** | **High** | A breach exposes other tenants' data -- fundamental trust requirement |
| QS-F01 Domain Integrity | **Must** | **High** | Invalid aggregates corrupt the domain model; cascading data issues |
| QS-S02 JWT Validation | **Must** | **High** | Without JWT validation, any request could impersonate users |
| QS-R01 Event Idempotency | **Must** | **High** | Duplicate events in production lead to data corruption |
| QS-M01 Framework-Free Domain | **Must** | **Medium** | Core architectural invariant; violation erodes hexagonal architecture |
| QS-S03 Role Enforcement | **Must** | **Medium** | Authorization bypass allows privilege escalation |
| QS-R02 Fault Isolation | **Must** | **Medium** | SCS independence is a core architecture promise |
| QS-M05 Architectural Conformance | **Must** | **Medium** | Without automated checks, architecture erodes over time |
| QS-P01 Page Load Time | **Should** | **Medium** | Users expect fast pages; SSR already helps but needs monitoring |
| QS-U01 Mobile-First | **Should** | **Medium** | Primary use case is mobile; poor mobile UX loses users |
| QS-R03 Dead Letter Handling | **Should** | **Medium** | Silent message loss leads to inconsistent read models |
| QS-S04 Input Sanitization | **Should** | **Low** | Thymeleaf escapes by default; risk is low but impact is high |
| QS-M03 Test Coverage | **Should** | **Medium** | TDD process mitigates risk, but coverage gaps can emerge |
| QS-F03 Event Contract Stability | **Should** | **Medium** | Breaking event changes require coordinated multi-SCS deployment |
| QS-P03 Event Processing Latency | **Should** | **Low** | Async by design; slight delays are acceptable |
| QS-U02 Ease of Use | **Should** | **Low** | Target audience is families; complexity loses users |
| QS-P04 Database Query Performance | **Should** | **Low** | Small data volumes currently; becomes important at scale |
| QS-M04 Onboarding Time | **Could** | **Low** | Small team; Docker Compose already simplifies setup |
| QS-U03 Offline Capability | **Could** | **Medium** | PWA not yet implemented; high effort, medium reward |
| QS-U04 Accessibility | **Could** | **Low** | PicoCSS provides good baseline; explicit WCAG not yet required |
| QS-PO1 Container Deployment | **Could** | **Low** | Docker Compose works; Kubernetes manifests incomplete |
| QS-S05 CSRF Protection | **Must** | **Low** | Spring Security enables by default; low risk of regression |

### Priority Quadrant Summary

```
                    High Risk              Low Risk
            ┌─────────────────────┬─────────────────────┐
  Must      │ QS-S01, QS-F01,    │ QS-M01, QS-S03,    │
            │ QS-S02, QS-R01     │ QS-R02, QS-M05,    │
            │                     │ QS-S05              │
            ├─────────────────────┼─────────────────────┤
  Should    │ QS-R03, QS-M03,    │ QS-P01, QS-U01,    │
            │ QS-F03              │ QS-S04, QS-P03,    │
            │                     │ QS-U02, QS-P04     │
            ├─────────────────────┼─────────────────────┤
  Could     │ QS-U03              │ QS-M04, QS-U04,    │
            │                     │ QS-PO1              │
            └─────────────────────┴─────────────────────┘
```

---

## 4. Metrics Dashboard

### 4.1 Security Metrics

| Metric | What to Measure | How to Measure | Target / SLO | Alert Condition |
|--------|----------------|----------------|--------------|-----------------|
| Tenant isolation violations | Cross-tenant data access attempts | Spring Security audit log + custom `AccessDeniedHandler` logging | 0 violations | Any violation triggers immediate alert |
| Authentication failure rate | Ratio of 401 responses to total requests | Micrometer `http.server.requests` counter filtered by status=401 | < 5% of total requests | > 10% in 5-minute window |
| JWT validation errors | Malformed or expired tokens | Spring Security `AuthenticationEntryPoint` counter (Micrometer) | < 1% of requests | > 5% in 5-minute window |
| OWASP dependency vulnerabilities | Known CVEs in dependencies | `org.owasp:dependency-check-maven` in CI pipeline | 0 Critical, 0 High | Any Critical or High CVE fails the build |

### 4.2 Reliability Metrics

| Metric | What to Measure | How to Measure | Target / SLO | Alert Condition |
|--------|----------------|----------------|--------------|-----------------|
| Event delivery success rate | Events published vs. consumed | RabbitMQ Management API metrics + Micrometer `rabbitmq.consumed` counter | > 99.9% delivery | < 99% in 1-hour window |
| Dead Letter Queue depth | Messages in DLQ | RabbitMQ Management API `queue.messages` for DLQ | 0 messages | > 0 messages |
| SCS availability | Per-SCS uptime | Spring Boot Actuator `/health` endpoint polled by Prometheus | 99.5% monthly uptime | Health check fails for > 30 seconds |
| Database connection pool utilization | Active vs. max connections | Micrometer `hikaricp.connections.active` / `hikaricp.connections.max` | < 80% utilization | > 90% for > 1 minute |

### 4.3 Performance Metrics

| Metric | What to Measure | How to Measure | Target / SLO | Alert Condition |
|--------|----------------|----------------|--------------|-----------------|
| Request latency (p95) | 95th percentile response time per SCS | Micrometer `http.server.requests` timer (Prometheus histogram) | p95 < 500ms | p95 > 1s for > 5 minutes |
| TTFB (Time to First Byte) | Server processing time for full-page renders | Micrometer `http.server.requests` timer for GET requests | < 300ms | > 500ms average over 5 minutes |
| Event processing latency | Time from event publish to consumer persistence | Custom Micrometer timer in `@TransactionalEventListener` and consumer | < 2s (p95) | > 5s (p95) |
| Flyway migration duration | Schema migration execution time | Flyway callback logging + Micrometer timer | < 30s per migration | > 60s |

### 4.4 Maintainability Metrics

| Metric | What to Measure | How to Measure | Target / SLO | Alert Condition |
|--------|----------------|----------------|--------------|-----------------|
| Test coverage per module | Line coverage | JaCoCo Maven plugin in CI | >= 80% per module | < 70% fails CI |
| Architectural violations | Hexagonal layer rule breaches | ArchUnit tests in each SCS (`domain` must not import `adapters`, `org.springframework`, `jakarta.persistence`) | 0 violations | Any violation fails CI |
| Cyclomatic complexity | Method-level complexity | SonarQube or SpotBugs analysis | < 10 per method | > 15 flagged in review |
| Build time | Full `./mvnw clean verify` duration | CI pipeline timer (GitHub Actions) | < 5 minutes | > 8 minutes |
| Dependency freshness | Days since last dependency update | Dependabot / `versions-maven-plugin` | < 90 days behind latest | > 180 days behind |

### 4.5 Usability Metrics

| Metric | What to Measure | How to Measure | Target / SLO | Alert Condition |
|--------|----------------|----------------|--------------|-----------------|
| Lighthouse Mobile score | Overall mobile quality | Lighthouse CI in pipeline against key pages | >= 90 | < 80 |
| Lighthouse Accessibility score | WCAG compliance | Lighthouse CI | >= 90 | < 80 |
| Page weight | Total transferred bytes per page | Lighthouse CI / browser DevTools | < 200 KB (no images) | > 500 KB |
| Touch target size | Minimum interactive element size | Manual review + Lighthouse audit | >= 44x44 px | Lighthouse flags small targets |

---

## 5. Trade-off Decisions

### 5.1 Consistency vs. Availability (CAP)

**Decision:** Favor availability over strong consistency across SCS boundaries.

- Within a single SCS: strong consistency via PostgreSQL transactions.
- Across SCS: eventual consistency via RabbitMQ events with at-least-once delivery.
- **Trade-off:** A TravelParty projection in Trips may lag behind a newly created Account in IAM by up to 2 seconds. This is acceptable because trip operations require the user to be already logged in (account exists).

### 5.2 Simplicity vs. Resilience (Event Infrastructure)

**Decision:** Accept simpler event infrastructure with manual DLQ intervention over automated retry/saga orchestration.

- No saga orchestration or outbox pattern (yet).
- Event publishing relies on `@TransactionalEventListener(AFTER_COMMIT)`. If RabbitMQ is temporarily unreachable after commit, the event is lost.
- **Accepted risk:** For a small-scale family application, the probability of event loss is low and the impact is recoverable by manual re-triggering.
- **Evolution path:** Introduce the Transactional Outbox pattern when reliability requirements increase (see Section 6).

### 5.3 Developer Productivity vs. Operational Complexity

**Decision:** Accept multiple databases and infrastructure components for domain isolation.

- 4 PostgreSQL instances + RabbitMQ + Keycloak is operationally complex for a small team.
- **Benefit:** Clean bounded context isolation, independent schema evolution, no cross-SCS coupling.
- **Mitigation:** Docker Compose abstracts local complexity; future Kubernetes deployment will use Helm charts.

### 5.4 Server-Side Rendering vs. Rich Client Interactivity

**Decision:** Thymeleaf + HTMX over SPA (React/Vue).

- **Benefit:** No separate frontend build, simpler deployment, better SEO, natural progressive enhancement.
- **Trade-off:** Limited offline capability (no client-side state management), less fluid animations, HTMX partial updates require server round-trips.
- **Mitigation:** PWA Service Worker can cache rendered HTML pages for offline viewing.

### 5.5 Security Strictness vs. Test Simplicity

**Decision:** Disable security in test profile (`@Profile("test")`) for faster, simpler tests.

- **Trade-off:** Integration tests do not validate security configuration (roles, JWT). Security is only tested in E2E tests against the full stack.
- **Accepted risk:** Security misconfigurations may not be caught until E2E. Mitigated by dedicated SecurityConfig with `@Profile("!test")` ensuring production config is explicit.
- **Evolution path:** Add dedicated security integration tests with `@WithMockUser` / `@WithMockJwtAuth` for critical endpoints.

---

## 6. Quality Evolution Roadmap

### Iteration 4 (Current -- v0.4.0-SNAPSHOT)

| Quality Focus | Actions |
|---------------|---------|
| **Architectural Conformance** | Add ArchUnit tests to enforce hexagonal layer rules in IAM, Trips, and Expense |
| **Test Coverage** | Integrate JaCoCo with per-module coverage thresholds (>= 80%) |
| **Event Reliability** | Implement Dead Letter Queue configuration for all consumer queues |
| **Tenant Isolation** | Add security integration tests verifying cross-tenant access returns 404 |
| **Responsive CSS** | Validate Mobile-First with Lighthouse CI on key pages |

### Iteration 5 (Planned)

| Quality Focus | Actions |
|---------------|---------|
| **Observability** | Add Micrometer + Prometheus metrics for request latency, event throughput, and DB pool usage |
| **Performance Baseline** | Establish p95 latency baselines for all page renders; add Lighthouse CI to pipeline |
| **Event Resilience** | Evaluate Transactional Outbox pattern to prevent event loss on RabbitMQ downtime |
| **Security Hardening** | Add OWASP dependency-check-maven to CI; add `@WithMockJwtAuth` security tests |
| **Accessibility** | Audit key flows for WCAG 2.1 Level AA compliance |

### Iteration 6+ (Future)

| Quality Focus | Actions |
|---------------|---------|
| **PWA / Offline** | Implement Service Worker with cache-first strategy for read-heavy pages (shopping list, trip details) |
| **Scalability** | Load test with representative data volumes (100 tenants, 500 trips); tune connection pools and indexes |
| **Kubernetes** | Complete Kubernetes manifests with health probes, resource limits, and HPA |
| **Alerting** | Set up Grafana dashboards with alert rules for all metrics defined in Section 4 |
| **Contract Testing** | Add Spring Cloud Contract or Pact tests for event contracts between SCS |

---

## Reference

![Quality Scenarios](../../design/evia.team.orc.thomas-klingler%20-%20Quality%20Scenarios.jpg)
