# 11. Risks and Technical Debt

## Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Keycloak Complexity** | High | Medium | Keycloak is powerful but complex to configure and operate. Realm configuration, theme customization, and multi-tenancy require deep knowledge. Mitigation: incremental configuration, documented realm settings, automation via Keycloak Admin CLI. |
| **SCS Boundary Evolution** | Medium | High | Bounded Context boundaries may prove suboptimal as the domain evolves. The Trips/Expense boundary is most likely to shift. Mitigation: review Context Mapping regularly, version event contracts, plan for refactoring. |
| **PWA Limitations** | Medium | Medium | PWAs have constraints vs. native apps (iOS Service Worker restrictions, limited camera access, no push notifications on iOS). Mitigation: graceful degradation, critical functions usable online, document iOS limitations. |
| **RabbitMQ Operational Complexity** | Low | Medium | Monitoring, exchange/queue management, and routing require experience. Mitigation: RabbitMQ Management UI, clear routing key conventions, Dead Letter Queues for failed events. |
| **Small Team Size** | High | Medium | A small team maintains three SCS plus infrastructure. Knowledge silos and bus factor are risks. Mitigation: ADRs, code reviews, pair programming, comprehensive documentation. |
| **Event Loss on Publish** | Medium | High | The current `@TransactionalEventListener(AFTER_COMMIT)` pattern can lose events if RabbitMQ is unreachable after the database transaction commits. The event is not persisted before sending. Mitigation: accept risk for now (low traffic, manual recovery possible). Evolution: Transactional Outbox pattern in Iteration 5+. |
| **No Automated Architectural Enforcement** | Medium | High | Hexagonal architecture invariants (framework-free domain, layer dependencies) are enforced by convention and code review only. Without ArchUnit tests, violations can creep in undetected. Mitigation: add ArchUnit tests in Iteration 4. |
| **Security Test Gap** | Medium | Medium | Security configuration (roles, JWT validation, tenant isolation) is not tested in integration tests due to disabled security in test profile. Misconfigurations are only caught in E2E tests. Mitigation: add dedicated security integration tests with `@WithMockJwtAuth` for critical endpoints. |
| **No Observability Stack** | High | Medium | No centralized logging, tracing, or metrics. Diagnosing production issues requires manual log inspection across multiple SCS. Mitigation: introduce Micrometer + Prometheus + Grafana in Iteration 5. |

## Technical Debt

| Debt | Description | Priority |
|------|-------------|----------|
| ~~**In-Memory Repositories**~~ | ~~Some repository implementations were still in-memory~~ -- **Resolved in Iteration 2**: IAM has complete JPA persistence adapters | ~~High~~ |
| **Missing Expense SCS Implementation** | The Expense Bounded Context domain and application layers are empty | Medium |
| **Security Disabled in Test Profile** | Integration tests do not validate security configuration (roles, JWT, tenant isolation) | Medium |
| ~~**Missing End-to-End Tests**~~ | ~~No automated E2E tests across SCS boundaries~~ -- **Addressed**: E2E module with Playwright created (see ADR-0010), base structure in place | ~~Medium~~ |
| **Monitoring and Observability** | No centralized logging, tracing, or metrics. No Micrometer/Prometheus integration. No alerting. | Medium |
| **Incomplete Kubernetes Manifests** | Deployment manifests only exist for IAM, not for all SCS. No health probes, resource limits, or HPA configured. | Medium |
| **No ArchUnit Tests** | Hexagonal architecture invariants (domain layer must not import Spring/JPA, adapter layer boundaries) are not enforced by automated tests | Medium |
| **No JaCoCo Coverage Enforcement** | Test coverage is not measured or enforced in the build pipeline. Coverage gaps may exist without visibility. | Low |
| **No Dead Letter Queue Configuration** | RabbitMQ consumers have no DLQ configured. Unprocessable messages cause infinite redelivery loops or are silently dropped depending on error handling. | Medium |
| **No OWASP Dependency Scanning** | Dependencies are not scanned for known CVEs in the CI pipeline. Vulnerable transitive dependencies may go undetected. | Low |
| **Event Contract Versioning** | Event contracts in `travelmate-common` have no versioning strategy. Breaking changes to events require coordinated deployment of all SCS. | Low |
| **No Transactional Outbox** | Event publishing after `@TransactionalEventListener(AFTER_COMMIT)` has no guaranteed delivery. Events can be lost if RabbitMQ is unavailable at publish time. | Low |
