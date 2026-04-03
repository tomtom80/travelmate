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
| ~~**No Automated Architectural Enforcement**~~ | ~~Medium~~ | ~~High~~ | **Resolved in Iteration 5 (v0.6.0)**: ArchUnit 1.3.0 fitness functions in all 3 SCS enforce hexagonal layer boundaries, domain purity, and naming conventions. |
| **Security Test Gap** | Medium | Medium | Security configuration (roles, JWT validation, tenant isolation) is not tested in integration tests due to disabled security in test profile. Misconfigurations are only caught in E2E tests. Mitigation: add dedicated security integration tests with `@WithMockJwtAuth` for critical endpoints. |
| **No Observability Stack** | High | Medium | No centralized logging, tracing, or metrics. Diagnosing production issues requires manual log inspection across multiple SCS. Mitigation: introduce Micrometer + Prometheus + Grafana in Iteration 5. |
| ~~**No HTMX Feedback Architecture**~~ | ~~High~~ | ~~High~~ | **Resolved in Iteration 5 (v0.6.0)**: ADR-0013 implementiert — Toast-Benachrichtigungen, HX-Trigger-basiertes Feedback, resiliente Event-Listener. |
| ~~**Event Publisher Exception Propagation**~~ | ~~Medium~~ | ~~High~~ | **Resolved in Iteration 5 (v0.6.0)**: DomainEventPublisher mit try-catch, ADR-0013. |
| ~~**No Logging in Exception Handlers**~~ | ~~High~~ | ~~Medium~~ | **Resolved in Iteration 5 (v0.6.0)**: i18n-fähige GlobalExceptionHandler mit Logging in allen 3 SCS. |

## Technical Debt

| Debt | Description | Priority |
|------|-------------|----------|
| ~~**In-Memory Repositories**~~ | ~~Some repository implementations were still in-memory~~ -- **Resolved in Iteration 2**: IAM has complete JPA persistence adapters | ~~High~~ |
| ~~**Missing Expense SCS Implementation**~~ | **Resolved in Iteration 5 (v0.6.0)**: Vollständiges Expense SCS mit Domain, Application, Adapters, 285 Tests, Party Settlement, Advance Payments, PDF Export, Receipt Scan | ~~Medium~~ |
| **Security Disabled in Test Profile** | Integration tests do not validate security configuration (roles, JWT, tenant isolation) | Medium |
| ~~**Missing End-to-End Tests**~~ | ~~No automated E2E tests across SCS boundaries~~ -- **Addressed**: E2E module with Playwright created (see ADR-0010), base structure in place | ~~Medium~~ |
| **Monitoring and Observability** | No centralized logging, tracing, or metrics. No Micrometer/Prometheus integration. No alerting. | Medium |
| **No Kubernetes Platform Baseline** | There is currently no maintained Kubernetes baseline in the repository: no manifests, no Helm/Kustomize structure, no ingress setup, and no deployment automation for a cluster target. | Medium |
| ~~**No ArchUnit Tests**~~ | **Resolved in Iteration 5 (v0.6.0)**: ArchUnit 1.3.0 in IAM, Trips, Expense | ~~Medium~~ |
| ~~**No JaCoCo Coverage Enforcement**~~ | **Resolved in Iteration 5 (v0.6.0)**: JaCoCo 0.8.12 Coverage Reporting im Parent POM | ~~Low~~ |
| ~~**No Dead Letter Queue Configuration**~~ | **Resolved in Iteration 5 (v0.6.0)**: DLQ mit exponential Backoff für alle Queues | ~~Medium~~ |
| **No OWASP Dependency Scanning** | Dependencies are not scanned for known CVEs in the CI pipeline. Vulnerable transitive dependencies may go undetected. | Low |
| **Event Contract Versioning** | Event contracts in `travelmate-common` have no versioning strategy. Breaking changes to events require coordinated deployment of all SCS. | Low |
| **No Transactional Outbox** | Event publishing after `@TransactionalEventListener(AFTER_COMMIT)` has no guaranteed delivery. Events can be lost if RabbitMQ is unavailable at publish time. | Low |
| **Duplicated GlobalExceptionHandler** | IAM and Trips have character-identical 77-line `GlobalExceptionHandler` classes. No shared pattern or base class. Changes must be applied to both SCS manually. | Low |
| ~~**Raw Exception Messages Exposed to Users**~~ | **Resolved in Iteration 5 (v0.6.0)**: i18n-fähige Fehlermeldungen in GlobalExceptionHandler | ~~Medium~~ |
| ~~**No HTMX Loading Indicators**~~ | **Resolved in Iteration 5 (v0.6.0)**: HTMX Loading States (aria-busy, disabled buttons) | ~~Medium~~ |
