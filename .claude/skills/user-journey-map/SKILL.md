---
name: User Journey Map
description: "Create user journey maps with personas, touchpoints, emotions, pain points, and opportunities for Travelmate user flows"
user-invocable: false
---

# User Journey Map Skill

Create structured user journey maps for Travelmate workflows.

## Personas

### Organizer (Keycloak role: organizer)
- Plans trips, manages travel party, invites participants
- Primary workflow: Dashboard → Create Trip → Invite → Manage

### Participant (Keycloak role: participant)
- Accepts invitations, views trip details, tracks expenses
- Primary workflow: Email Invitation → Accept → View Trip → Add Expenses

### New User (unauthenticated)
- Discovers the platform, signs up, creates first travel party
- Primary workflow: Landing → Sign-Up → Email Verification → Login → Dashboard

## Journey Map Template

```markdown
## User Journey: [Journey Name]
**Persona**: [Organizer / Participant / New User]
**Goal**: [Primary goal]
**Trigger**: [What initiates this journey]

### Journey Phases

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. [Phase] | [What user does] | [What system does] | [Page/Component] | [😀😐😟] | [Friction] | [Improvement] |
| 2. ... | ... | ... | ... | ... | ... | ... |

### Key Metrics
| Metric | Target | Measurement |
|--------|--------|-------------|
| Task completion rate | >95% | Analytics |
| Time to complete | <X min | User testing |
| Error rate | <5% | Error logs |

### Improvement Opportunities
1. [Priority: High] [Description]
2. [Priority: Medium] [Description]
```

## Output Location
- Journey maps → `docs/design/journeys/`
