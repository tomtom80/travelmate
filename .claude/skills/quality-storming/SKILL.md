---
name: Quality Storming
description: "Discover and prioritize quality requirements using INNOQ's Quality Storming methodology, derive observable metrics for each quality attribute"
user-invocable: false
---

# Quality Storming Skill

Discover and evaluate architecture quality attributes using the Quality Storming methodology from INNOQ.

References:
- https://www.innoq.com/de/articles/2020/03/quality-storming-workshop/
- https://www.innoq.com/de/blog/2021/10/quality-value-chain-evolution/
- https://www.innoq.com/de/podcast/146-architekturqualitaet/

## Workshop Process

### Phase 1: Quality Discovery
1. **Brainstorm** quality attributes relevant to Travelmate
2. **Categorize** using ISO 25010 quality model:
   - Functional Suitability, Performance Efficiency, Compatibility, Usability
   - Reliability, Security, Maintainability, Portability

### Phase 2: Quality Scenarios
For each quality attribute, define concrete scenarios:

```markdown
### Quality Scenario: [QS-NNN]
**Quality Attribute**: [e.g., Security — Tenant Isolation]
**Stimulus**: A user tries to access another tenant's trip data
**Environment**: Production, normal load
**Response**: System returns 403 Forbidden, logs security event
**Response Measure**: Zero cross-tenant data leakage in 100% of attempts
```

### Phase 3: Priority & Trade-offs
Rate each scenario: **Priority** (Must/Should/Could) × **Risk** (High/Medium/Low)

Create a priority matrix:
```
         High Risk    Medium Risk    Low Risk
Must     ████████     ██████         ████
Should   ██████       ████           ██
Could    ████         ██             █
```

### Phase 4: Observable Metrics

**CRITICAL**: For every quality attribute, define metrics that can be measured:

```markdown
### Metric: [M-NNN] [Metric Name]
**Quality Scenario**: QS-NNN
**What to Measure**: [concrete measurement]
**How to Measure**: [tool/method]
**Target**: [threshold/SLO]
**Alert Condition**: [when to alert]
```

#### Example Metrics for Travelmate
| Quality | Metric | Target | Tool |
|---------|--------|--------|------|
| Performance | P95 response time | < 500ms | Micrometer/Prometheus |
| Security | Cross-tenant access attempts | 0 successful | Security audit log |
| Reliability | Event delivery success rate | > 99.9% | RabbitMQ monitoring |
| Maintainability | Cyclomatic complexity per method | < 10 | SonarQube / ArchUnit |
| Usability | Time to complete trip creation | < 2 minutes | User analytics |

### Phase 5: Quality Value Chain Evolution
Map how quality attributes evolve over iterations:

```
Iteration 1: Security (authentication) → Basic
Iteration 2: Security (tenant isolation) → Robust
Iteration 3: Performance (caching) → Optimized
Iteration 4: Reliability (event delivery) → Resilient
```

## Output Format

```markdown
# Quality Storming Results — Travelmate

## Quality Attributes (prioritized)
[Ranked list with categories]

## Quality Scenarios
[Detailed scenarios with response measures]

## Metrics Dashboard
[Table of metrics with targets and measurement methods]

## Trade-off Decisions
[Documented trade-offs as input for ADRs]

## Evolution Roadmap
[Quality improvement plan across iterations]
```

## Integration Points
- Quality scenarios → `docs/arc42/10-quality-requirements.md`
- Trade-off decisions → new ADRs in `docs/adr/`
- Metrics → monitoring configuration (Micrometer/Prometheus)
- Architecture fitness functions → ArchUnit tests
