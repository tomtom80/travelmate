---
name: SRE Practices
description: "Apply Google Site Reliability Engineering practices — SLOs/SLIs/SLAs, error budgets, toil reduction, incident management, capacity planning, and reliability patterns"
user-invocable: false
---

# Site Reliability Engineering Practices Skill

Apply SRE principles from Google's SRE Book (https://sre.google/sre-book/table-of-contents/).

## Core SRE Principles

### 1. Service Level Objectives (SLOs / SLIs / SLAs)

**SLI (Service Level Indicator)** — A quantitative measure of service behavior.
**SLO (Service Level Objective)** — A target value for an SLI (internal agreement).
**SLA (Service Level Agreement)** — A business contract with consequences for missing SLOs.

#### Travelmate SLI/SLO Definitions

| Service | SLI | SLO Target | Measurement |
|---------|-----|------------|-------------|
| **Gateway** | Availability (successful requests / total requests) | 99.9% (43.8 min/month downtime) | Prometheus `http_server_requests_seconds_count` |
| **Gateway** | Latency (P99 response time) | < 1000ms | Prometheus `http_server_requests_seconds` histogram |
| **IAM** | Sign-up success rate | 99.5% | Application metrics + error logs |
| **IAM** | Login latency (P95) | < 500ms | Micrometer timer |
| **Trips** | Trip creation success rate | 99.9% | Application metrics |
| **Trips** | Event processing latency (IAM → TravelParty projection) | < 5s (P99) | RabbitMQ consumer lag |
| **RabbitMQ** | Message delivery rate | 99.99% | RabbitMQ monitoring (unacked messages) |
| **Keycloak** | Token issuance latency (P95) | < 300ms | Keycloak metrics |
| **All SCS** | Error rate (5xx responses) | < 0.1% | Prometheus error rate |

#### SLO Document Template
```markdown
# SLO Document — [Service Name]

## Service Overview
- **Owner**: [team/person]
- **Dependencies**: [upstream/downstream services]
- **User-facing**: [yes/no]

## SLIs
| SLI | Definition | Good Event | Valid Event |
|-----|-----------|------------|-------------|

## SLOs
| SLO | Target | Window | Consequence of Breach |
|-----|--------|--------|----------------------|

## Error Budget
- **Budget**: 100% - SLO target (e.g., 99.9% → 0.1% error budget)
- **Budget consumption**: [current period usage]
- **Policy when exhausted**: [freeze features / focus on reliability]
```

### 2. Error Budgets

Error Budget = 1 - SLO target. It quantifies how much unreliability is acceptable.

```
Example: 99.9% availability SLO
→ Error budget: 0.1% = 43.8 minutes/month
→ If budget is exhausted: freeze feature releases, focus on reliability
→ If budget is healthy: ship features faster
```

#### Error Budget Policy for Travelmate
```markdown
## Error Budget Policy

### Budget Healthy (> 50% remaining)
- Normal feature development velocity
- Standard deployment frequency

### Budget Warning (25-50% remaining)
- Increase code review rigor
- Add reliability-focused tests
- Review recent incidents for patterns

### Budget Critical (< 25% remaining)
- Feature freeze — reliability work only
- Mandatory incident postmortem for every outage
- Architecture review for systemic issues

### Budget Exhausted (0%)
- Complete feature freeze
- All engineering effort on reliability
- Rollback recent changes if they contributed
```

### 3. Eliminating Toil (Chapter 5)

**Toil** = manual, repetitive, automatable, tactical, no enduring value, scales linearly.

#### Toil Identification for Travelmate
| Task | Toil? | Automation |
|------|-------|-----------|
| Database backups | Yes | Cron job / managed service |
| Keycloak realm config changes | Yes | Terraform/Ansible + realm-export.json |
| SSL certificate renewal | Yes | Let's Encrypt + cert-manager |
| Log rotation | Yes | Logrotate / managed logging |
| Dependency updates | Partial | Dependabot / Renovate |
| E2E test environment setup | Yes | Docker Compose + CI pipeline |
| Incident triage | No | Requires human judgment |
| Capacity planning | No | Requires analysis |

**Target**: < 50% of operational work should be toil (Google SRE standard).

### 4. Monitoring & Alerting (Chapters 6, 10)

#### The Four Golden Signals
1. **Latency** — Time to service a request (distinguish success vs. error latency)
2. **Traffic** — Demand on the system (requests/sec per SCS)
3. **Errors** — Rate of failed requests (explicit 5xx + implicit timeout/wrong content)
4. **Saturation** — How "full" the service is (CPU, memory, DB connections, queue depth)

#### Travelmate Monitoring Stack
```
Spring Boot Actuator → Micrometer → Prometheus → Grafana
                                                    ↓
                                              Alertmanager → PagerDuty/Slack/Email
```

#### Alert Design Principles
- **Symptom-based, not cause-based** — alert on "high error rate" not "disk full"
- **Actionable** — every alert requires a human action
- **No alert fatigue** — tune thresholds, use multi-window burn rate alerts
- **Escalation** — warning → critical → page

#### Multi-Window, Multi-Burn-Rate Alerts (Chapter 5, SRE Workbook)
```yaml
# Fast burn: consuming error budget 14.4x faster than sustainable
# → Alert within 1 hour of budget exhaustion rate
- alert: HighErrorRateFastBurn
  expr: |
    (rate(http_requests_total{status=~"5.."}[1h]) / rate(http_requests_total[1h])) > (14.4 * 0.001)
    and
    (rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])) > (14.4 * 0.001)

# Slow burn: consuming error budget 3x faster than sustainable
# → Alert for gradual degradation
- alert: HighErrorRateSlowBurn
  expr: |
    (rate(http_requests_total{status=~"5.."}[6h]) / rate(http_requests_total[6h])) > (3 * 0.001)
    and
    (rate(http_requests_total{status=~"5.."}[30m]) / rate(http_requests_total[30m])) > (3 * 0.001)
```

### 5. Incident Management (Chapter 14)

#### Incident Response Process
```
Detection → Triage → Mitigation → Resolution → Postmortem
```

#### Postmortem Template (Blameless)
```markdown
# Incident Postmortem — [Incident Title]

**Date**: [YYYY-MM-DD]
**Duration**: [start - end]
**Severity**: [S1/S2/S3/S4]
**Author**: [name]
**Status**: [Draft / Final]

## Summary
[1-2 sentence description]

## Impact
- **Users affected**: [count/percentage]
- **SLO impact**: [error budget consumed]
- **Revenue impact**: [if applicable]

## Timeline
| Time | Event |
|------|-------|
| HH:MM | [What happened] |

## Root Cause
[Technical root cause — not "human error"]

## Contributing Factors
- [Factor 1]
- [Factor 2]

## What Went Well
- [Positive aspects of incident response]

## What Went Wrong
- [Issues that made the incident worse or slower to resolve]

## Action Items
| Action | Owner | Priority | Deadline | Status |
|--------|-------|----------|----------|--------|
| [Fix] | [Who] | [P0-P3] | [Date] | [Open/Done] |

## Lessons Learned
[Key takeaways for the team]
```

### 6. Capacity Planning (Chapter 18)

#### Travelmate Capacity Dimensions
| Resource | Current | Growth Factor | Scaling Strategy |
|----------|---------|---------------|------------------|
| PostgreSQL connections | 4 × HikariCP pool (10) | Per-tenant growth | Connection pooling, read replicas |
| RabbitMQ throughput | Low (event-driven) | Per-trip activity | Queue partitioning |
| JVM heap per SCS | 256MB-512MB | Per-concurrent-users | Horizontal scaling (multiple instances) |
| Keycloak sessions | Per-concurrent-users | Linear | Clustered Keycloak |
| Docker container count | 10 services | Per-environment | Orchestration (K8s/ECS) |

#### Load Testing
- Use tools like k6, Gatling, or JMeter
- Test against realistic traffic patterns (trip creation spikes, login bursts)
- Establish performance baselines before each release

### 7. Release Engineering (Chapter 8)

#### Deployment Strategies for Travelmate
| Strategy | Risk | Rollback Speed | Complexity |
|----------|------|----------------|------------|
| **Rolling Update** | Medium | Fast | Low |
| **Blue/Green** | Low | Instant | Medium |
| **Canary** | Very Low | Fast | High |

#### Recommended: Rolling Update per SCS
- SCS are independently deployable (SCS architecture benefit)
- Deploy one SCS at a time, verify SLOs before proceeding
- Flyway migrations must be backwards-compatible (expand/contract pattern)

### 8. Reliability Patterns

#### Circuit Breaker (SCS → External Systems)
- Keycloak Admin API calls should use circuit breaker
- RabbitMQ publisher confirms with retry and dead-letter queue

#### Graceful Degradation
- If Trips SCS is down → IAM dashboard still works (SCS independence)
- If RabbitMQ is down → events are buffered (Transactional Outbox pattern)
- If Keycloak is down → existing JWTs still valid until expiry

#### Health Checks
```yaml
# Existing in docker-compose.yml — ensure Spring Boot Actuator health includes:
management.health.rabbit.enabled=true
management.health.db.enabled=true
# Custom health indicator for Keycloak connectivity
```

## Workflow

1. **Define SLIs/SLOs** from Quality Storming results and business requirements
2. **Implement monitoring** using Spring Boot Actuator + Micrometer + Prometheus
3. **Configure alerting** with multi-window burn rate alerts
4. **Establish error budgets** and budget policies
5. **Identify and eliminate toil** through automation
6. **Plan capacity** based on growth projections
7. **Document incident response** procedures and postmortem templates

## Output Locations

- SLO documents → `docs/operations/slos/`
- Runbooks → `docs/operations/runbooks/`
- Postmortem templates → `docs/operations/postmortems/`
- Monitoring config → `infrastructure/monitoring/`
- Alert rules → `infrastructure/monitoring/alerts/`
- Capacity plans → `docs/operations/capacity/`
