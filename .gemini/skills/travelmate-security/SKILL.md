---
name: travelmate-security
description: "Threat modeling, security code reviews, and penetration testing. Use when discussing authentication, authorization, input validation, tenant isolation, or security hardening."
---

# Travelmate Security Skill

Application security expertise for the Travelmate project.

## Core Mandates
- Every SCS is an OAuth2 Resource Server.
- Gateway is the sole OIDC entry point (TokenRelay).
- **CRITICAL**: Tenant isolation must be scoped by `TenantId` from JWT claims.

## Specialized Workflows

### 🕵️ 1. Threat Modeling (STRIDE)
- Analyze data flows: Gateway <-> SCS, SCS <-> RabbitMQ, SCS <-> DB.
- Assets: JWTs, personal info, travel plans, financial data.
- **Reference**: See [security-expert.md](references/security-expert.md).

### 🔍 2. Security Code Review (OWASP Top 10)
- Broken Access Control (tenant isolation).
- Injection (JPA queries, Thymeleaf).
- SSRF (check external call points).
- **Reference**: See [security-expert.md](references/security-expert.md).

### 🛡️ 3. Penetration Testing
- Authentication/Authorization flows.
- Input validation on HTMX endpoints.
- Business logic (trip lifecycle, invitations).
- **Reference**: See [security-expert.md](references/security-expert.md).

### 📄 4. Security Reporting
- Produce structured assessment reports.
- Risk matrix (Likelihood x Impact).
- Prioritized remediation plan.
