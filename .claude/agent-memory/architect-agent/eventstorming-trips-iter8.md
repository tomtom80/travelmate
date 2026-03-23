---
name: EventStorming Trips Iteration 8
description: Design-level EventStorming for Shopping List (Einkaufsliste) aggregate and Email Notification extension. Key decisions on ShoppingList as persisted aggregate, lazy regeneration, ingredient matching, and email notification staying in Trips SCS.
type: project
---

Completed Design-Level EventStorming for Trips SCS Iteration 8 features on 2026-03-17.

**Key Design Decisions**:
- ShoppingList is a NEW persisted Aggregate (not a stateless computation) -- needed for Assignment, Purchase-Status, Manual Items
- ShoppingItem is an Entity within ShoppingList (like Receipt within Expense)
- Regeneration trigger: Lazy (explicit button), not eager (no MealPlan events needed)
- Ingredient matching: case-insensitive (name+unit), no fuzzy/NLP
- RECIPE items get NEW IDs on regeneration (no stable ID diff algorithm)
- Email notification stays in Trips SCS (InvitationEmailListener pattern)
- New local event: InvitationNotificationRequested (multi-recipient, not in common)
- No new event contracts in travelmate-common, no new RoutingKeys
- No cross-SCS changes needed (everything in Trips SCS)
- participantCount for scaling = Trip.participants.size() (simple, no StayPeriod weighting)

**Artifacts**: `docs/design/eventstorming/trips-iteration-8.md` + 2 PlantUML diagrams

**Why:** Iteration 8 planning requires tactical DDD modeling before TDD implementation.

**How to apply:** Use these design decisions when implementing Shopping List and Email notification stories. ADR for ShoppingList aggregate design still needs to be written.
