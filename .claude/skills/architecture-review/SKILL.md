---
name: Architecture Review
description: "Evaluate architecture fitness using ATAM and embarc methodology — systematic quality attribute analysis, risk identification, and conformance checking"
user-invocable: false
---

# Architecture Review Skill

Evaluate architecture using methodology from https://www.embarc.de/themen/architekturbewertung/

## Review Methods

### 1. ATAM (Architecture Tradeoff Analysis Method)
1. **Present Architecture** — Review arc42 documentation
2. **Identify Quality Goals** — From `docs/arc42/10-quality-requirements.md`
3. **Generate Quality Scenarios** — Concrete, measurable scenarios
4. **Analyze Scenarios** — Map to architecture decisions, identify trade-offs
5. **Identify Risks** — Architecture risks from trade-offs
6. **Present Results** — Prioritized findings with recommendations

### 2. Conformance Check
Verify implementation matches documented architecture:

| Invariant | Check Method |
|-----------|-------------|
| Domain layer framework-free | ArchUnit test + manual review |
| TenantId aggregate isolation | Grep all repositories for TenantId parameter |
| Async SCS communication | Check for no direct HTTP calls between SCS |
| Database isolation | Verify 4 separate datasources |
| Flyway schema ownership | Check ddl-auto=validate in all profiles |
| Server-side rendering | No SPA frameworks in dependencies |
| Event publishing pattern | Review @TransactionalEventListener usage |
| Shared Kernel purity | Review travelmate-common for business logic |

### 3. Risk Assessment Matrix

```
Impact →     Low          Medium        High          Critical
Likelihood
─────────────────────────────────────────────────────────────
Very Likely  Medium       High          Critical      Critical
Likely       Low          Medium        High          Critical
Unlikely     Low          Low           Medium        High
Rare         Low          Low           Low           Medium
```

## Report Template

```markdown
# Architecture Review Report — Travelmate v[X.Y.Z]

## 1. Review Scope
[Modules/aspects reviewed]

## 2. Architecture Overview
[Current state summary]

## 3. Quality Attribute Analysis
[Scenario-based evaluation]

## 4. Conformance Results
| Invariant | Status | Evidence | Notes |
|-----------|--------|----------|-------|

## 5. Risks
| ID | Risk | Impact | Likelihood | Mitigation |
|----|------|--------|------------|------------|

## 6. Technical Debt
| ID | Debt Item | Severity | Effort | Recommendation |
|----|-----------|----------|--------|----------------|

## 7. Recommendations (prioritized)
1. [Critical] ...
2. [High] ...
3. [Medium] ...
```

## Output Location
- Review reports → `docs/arc42/11-risks-and-technical-debt.md` (update)
- Detailed reports → `reports/architecture-review-YYYY-MM-DD.md`
