---
name: EventStorming
description: "Facilitate EventStorming sessions — discover domain events, commands, aggregates, and bounded context boundaries using Alberto Brandolini's methodology"
user-invocable: false
---

# EventStorming Skill

Facilitate EventStorming workshops (Big Picture, Process-Level, Design-Level) following Alberto Brandolini's methodology.

## Color Coding (Sticky Notes)
- 🟠 **Orange** — Domain Events (past tense: "OrderPlaced", "TripCreated")
- 🔵 **Blue** — Commands (imperative: "CreateTrip", "InviteParticipant")
- 🟡 **Yellow** — Aggregates (nouns: Trip, Invitation, TravelParty)
- 🟣 **Purple/Lilac** — Policies/Process Managers ("When X, then Y")
- 🟢 **Green** — Read Models / Views
- 🔴 **Red** — Hot Spots / Questions / Problems
- 🩷 **Pink** — External Systems (Keycloak, RabbitMQ)
- 🤏 **Small Yellow** — Actors/Roles (Organizer, Participant)

## Big Picture EventStorming
1. **Chaotic Exploration** — List ALL domain events in the system
2. **Timeline** — Arrange events chronologically
3. **Swimlanes** — Group by bounded context
4. **Hot Spots** — Mark uncertainties and conflicts
5. **Bounded Contexts** — Draw context boundaries

## Process-Level EventStorming
1. Start with a specific business process (e.g., "Trip Invitation Flow")
2. Add Commands that trigger Events
3. Add Actors who issue Commands
4. Add Policies that react to Events
5. Add Read Models that inform decisions

## Design-Level EventStorming
1. Focus on a single Aggregate
2. Map Command → Aggregate → Event(s)
3. Define invariants the Aggregate enforces
4. Identify Value Objects within the Aggregate

## Output Format

Generate a structured markdown document with PlantUML activity diagrams:

```markdown
## EventStorming: [Process Name]

### Domain Events (chronological)
1. TenantCreated → 2. AccountRegistered → 3. TripCreated → ...

### Command-Event Mapping
| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| CreateTrip | Organizer | Trip | TripCreated | — |

### Bounded Context Map
[PlantUML context map diagram]

### Hot Spots
- [ ] [Question or uncertainty]
```

## Travelmate Events (existing)
- IAM: TenantCreated, AccountRegistered, MemberAddedToTenant, DependentAddedToTenant, DependentRemovedFromTenant, MemberRemovedFromTenant, TenantDeleted
- Trips: TripCreated, ParticipantJoinedTrip, TripCompleted
