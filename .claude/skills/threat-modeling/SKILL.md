---
name: Threat Modeling
description: "Perform STRIDE-based threat model analysis following OWASP methodology, produce comprehensive threat model documents"
user-invocable: false
---

# Threat Modeling Skill

Perform threat model analysis following OWASP methodology (https://owasp.org/www-community/Threat_Modeling).

Reference document structure: https://github.com/corona-warn-app/cwa-documentation/blob/main/overview-security.md

## STRIDE Threat Categories

| Category | Property Violated | Example in Travelmate |
|----------|-------------------|------------------------|
| **S**poofing | Authentication | Forged JWT, stolen session |
| **T**ampering | Integrity | Modified request body, tampered events |
| **R**epudiation | Non-repudiation | Unlogged state changes |
| **I**nformation Disclosure | Confidentiality | Cross-tenant data leak, verbose errors |
| **D**enial of Service | Availability | Resource exhaustion, queue flooding |
| **E**levation of Privilege | Authorization | Role escalation, tenant boundary bypass |

## Travelmate Data Flow Diagram

```
┌──────────┐  HTTPS   ┌──────────┐  HTTP/JWT  ┌──────────┐
│  Browser  │────────▶│  Gateway  │──────────▶│   IAM    │──▶ PostgreSQL (5432)
│           │         │  (:8080)  │           │  (:8081) │
└──────────┘         └──────────┘           └──────────┘
                          │                       │
                          │  HTTP/JWT              │ AMQP
                          ▼                       ▼
                     ┌──────────┐          ┌──────────┐
                     │  Trips   │◀────────│ RabbitMQ │
                     │  (:8082) │         │  (:5672) │
                     └──────────┘         └──────────┘
                          │                    ▲
                     PostgreSQL (5433)          │ AMQP
                                          ┌──────────┐
                     ┌──────────┐         │ Expense  │
                     │ Keycloak │         │  (:8083) │
                     │  (:7082) │         └──────────┘
                     └──────────┘              │
                                         PostgreSQL (5434)
```

## Trust Boundaries

1. **Internet ↔ Gateway** — TLS termination, OIDC authentication
2. **Gateway ↔ SCS** — JWT token relay, internal network
3. **SCS ↔ Database** — Credentials, connection pool
4. **SCS ↔ RabbitMQ** — Message integrity, authentication
5. **SCS ↔ Keycloak** — Admin API credentials, OIDC tokens

## Threat Enumeration Template

```markdown
### T-[NNN]: [Threat Title]
**Category**: [STRIDE category]
**Component**: [Affected component]
**Trust Boundary**: [Which boundary]
**Attack Vector**: [How the attack works]
**Impact**: [Critical / High / Medium / Low]
**Likelihood**: [Very Likely / Likely / Unlikely / Rare]
**Risk Rating**: [Impact × Likelihood]
**Current Mitigation**: [Existing countermeasure or "None"]
**Recommended Mitigation**: [What to implement]
**Status**: [Open / Mitigated / Accepted / Transferred]
```

## DREAD Scoring (alternative to Impact × Likelihood)

| Factor | 1 (Low) | 2 (Medium) | 3 (High) |
|--------|---------|------------|----------|
| **D**amage | Minor data exposure | Significant data loss | Complete system compromise |
| **R**eproducibility | Difficult, specific conditions | Moderate effort | Trivially reproducible |
| **E**xploitability | Expert knowledge required | Some skill needed | Script kiddie level |
| **A**ffected Users | Single user | Tenant | All tenants |
| **D**iscoverability | Obscure attack surface | Some investigation | Obvious vulnerability |

Score = (D + R + E + A + D) / 5 → Low (1-1.5), Medium (1.5-2.5), High (2.5-3)

## OWASP Four-Question Framework

Every threat model MUST answer these four questions:
1. **What are we working on?** — Scope, DFDs, entry/exit points, assets, trust levels
2. **What can go wrong?** — STRIDE per element, attack trees
3. **What are we going to do about it?** — Countermeasures, risk response (Accept/Eliminate/Mitigate/Transfer)
4. **Did we do a good job?** — Verify records, confirm coverage

## Scoping Artifacts (Step 1)

### Entry Points (hierarchical)
```markdown
| ID | Name | Description | Trust Levels |
|----|------|-------------|-------------|
| 1 | HTTPS Port (8080) | Gateway entry | Anonymous Web User |
| 1.1 | Login Page | Keycloak OIDC redirect | Anonymous Web User |
| 1.2 | Dashboard | Main authenticated view | User with Valid Credentials |
| 1.2.1 | Trip Creation | Create new trip | User with Organizer Role |
```

### Assets
```markdown
| ID | Name | Description | Trust Levels |
|----|------|-------------|-------------|
| A1 | User Credentials | Keycloak-managed passwords | Keycloak Admin |
| A2 | JWT Tokens | Bearer tokens for SCS access | Authenticated User |
| A3 | Tenant Data | Travel party members, trips | Tenant Members Only |
| A4 | Personal Info | Names, DOB, email | Data Owner + Tenant Organizer |
| A5 | Financial Data | Expenses, receipts | Tenant Members Only |
```

### Trust Levels
```markdown
| ID | Name | Description |
|----|------|-------------|
| TL1 | Anonymous Web User | Unauthenticated browser user |
| TL2 | User with Valid Credentials | Authenticated via Keycloak |
| TL3 | Organizer | Keycloak role: organizer |
| TL4 | Participant | Keycloak role: participant |
| TL5 | Keycloak Admin | Keycloak realm administrator |
| TL6 | Database User | PostgreSQL connection credentials |
| TL7 | RabbitMQ User | AMQP connection credentials |
```

## STRIDE-to-Mitigation Mapping

| Threat | Standard Mitigations |
|--------|---------------------|
| Spoofing | Authentication, protect secrets, don't store secrets |
| Tampering | Authorization, hashes, MACs, digital signatures, tamper-resistant protocols |
| Repudiation | Digital signatures, timestamps, audit trails |
| Information Disclosure | Authorization, encryption, privacy-enhanced protocols |
| Denial of Service | Authentication, authorization, filtering, throttling, QoS |
| Elevation of Privilege | Run with least privilege, input validation |

## CWA-Style Cross-Referencing (Risk ↔ Threat)

Use HTML anchors for many-to-many risk-to-threat mapping:
```markdown
### Technical Risks
- <a name="risk-tenant-isolation">Cross-Tenant Data Access</a>
  - Related threats:
    - [JWT Email Claim Manipulation](#t-001)
    - [Missing TenantId Filter in Query](#t-002)

### Threats and Proposed Controls
- <a name="t-001">JWT Email Claim Manipulation</a>
  - Proposed controls:
    - Validate JWT signature against Keycloak public key
    - Verify email claim format and existence
```

## Output Format

```markdown
# Threat Model — Travelmate [Component/System]

## 1. Scope & Objectives
## 2. System Overview (with DFD)
## 3. Entry Points & Exit Points
## 4. Assets Inventory
## 5. Trust Levels
## 6. Trust Boundaries
## 7. Threat Enumeration (STRIDE per element)
  ### Use-Case-Independent Threats
  ### [Feature-Specific Threat Groups]
## 8. Risk Assessment Matrix
## 9. Current Countermeasures
## 10. Gaps & Recommendations
## 11. Remediation Roadmap
## 12. Security Testing Plan (SAST, dependency scan, manual testing)
## 13. Secure Operations Considerations
```

## Lifecycle

Threat models MUST be updated after:
- New features or bounded contexts added
- Security incidents
- Architecture or infrastructure changes
- Dependency major version upgrades
