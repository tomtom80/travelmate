---
name: Domain Storytelling
description: "Model domain workflows using Domain Storytelling notation — actors, work objects, and activities in scenario-based narratives"
user-invocable: false
---

# Domain Storytelling Skill

Model domain workflows using the Domain Storytelling method by Stefan Hofer & Henning Schwentner.
Tool reference: https://egon.io/

## Notation Elements
- **Actors** — People or systems that perform activities (e.g., Organizer, Participant, Keycloak)
- **Work Objects** — Things that actors work with (e.g., Trip, Invitation, Account)
- **Activities** — Actions connecting actors to work objects (numbered sequentially)
- **Annotations** — Additional notes on activities or objects

## Storytelling Process
1. **Pick a Scenario** — One concrete story (e.g., "Organizer creates a trip and invites participants")
2. **Tell the Story** — Narrate step by step with numbered activities
3. **Draw the Diagram** — Actors on the left, flow to the right
4. **Identify Boundaries** — Where do handoffs happen? → Bounded Context boundaries
5. **Iterate** — Tell variations (error cases, alternative flows)

## Story Format

```markdown
## Domain Story: [Story Title]
**Scope**: [coarse-grained / fine-grained]
**Variant**: [pure / digitalized]

### Story
1. **Organizer** creates a **Trip** with name, dates, and description
2. **Organizer** sends an **Invitation** for the **Trip** to a **Participant** email
3. **System** delivers **Invitation Email** to **Participant**
4. **Participant** accepts the **Invitation**
5. **System** adds **Participant** to the **Trip**
6. **Organizer** sees updated **Participant List** on **Trip Details**

### Bounded Context Boundaries
- Steps 1-2, 6: Trips Context
- Step 3: Messaging/Email (External System)
- Step 4-5: Trips Context (Invitation subdomain)

### Derived Domain Objects
| Object | Type | Bounded Context |
|--------|------|-----------------|
| Trip | Aggregate Root | Trips |
| Invitation | Aggregate Root | Trips |
| Participant | Entity (within Trip) | Trips |
```

## Egon.io Export
When possible, suggest the user model the story in Egon.io and export as SVG/PNG to `docs/design/stories/`.

## Travelmate Actors
- Organizer (Keycloak role: organizer)
- Participant (Keycloak role: participant)
- New User (unauthenticated)
- System (Gateway, SCS backends)
- Keycloak (external identity provider)
