---
description: "Run the Security Expert Agent for threat modeling, security code review, or penetration testing"
argument-hint: "[task: threat-model | code-review | pentest] [scope: full | iam | trips | expense | gateway]"
model: opus
---

# Security Command

You are orchestrating the Security Expert Agent. Based on the user's request, invoke the appropriate security workflow.

## Available Tasks

| Task | Description |
|------|-------------|
| `threat-model` | Perform STRIDE-based threat model analysis |
| `code-review` | Security code review against OWASP Top 10 |
| `pentest` | Penetration testing against local infrastructure |

## Scope Options

| Scope | Description |
|-------|-------------|
| `full` | Entire Travelmate system |
| `iam` | IAM bounded context (authentication, tenants, accounts) |
| `trips` | Trips bounded context (trips, invitations, travel parties) |
| `expense` | Expense bounded context (ledgers, receipts) |
| `gateway` | Gateway (OIDC, routing, token relay) |

## Instructions

1. Parse the arguments: `$ARGUMENTS`
2. If no task specified, ask the user which security task they want
3. Spawn the `security-expert-agent` using the Agent tool with `subagent_type: security-expert-agent`
4. Provide the agent with the task and scope
5. Present findings in a structured security report

## Important
- Penetration testing is ONLY for the local development environment
- All testing is authorized security assessment for defensive purposes
- Reports go to `reports/` directory
