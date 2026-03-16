---
name: EventStorming Expense Iteration 6
description: Design-level EventStorming for Receipt Review (four-eyes), Settlement Calculation (debt simplification), Expense Categories, and Accommodation Splitting (by nights). Key decisions on aggregate design, new events, and cross-SCS StayPeriod flow.
type: project
---

Completed Design-Level EventStorming for Expense SCS Iteration 6 features on 2026-03-16.

**Key Design Decisions**:
- Receipt stays as Entity within Expense aggregate (no separate aggregate)
- SettlementPlan is a Value Object within Expense (not separate aggregate)
- ExpenseStatus gets READY_FOR_SETTLEMENT intermediate state (reversible)
- Four-Eyes: reviewerId != submittedBy (not paidBy); submittedBy can differ from paidBy
- SplitStrategy enum (WEIGHTED vs BY_NIGHTS) on Receipt; NightAllocation frozen at creation time
- New StayPeriodUpdated event from Trips SCS needed for accommodation splitting
- Receipt review events (ReceiptSubmitted/Approved/Rejected) are internal-only, not published to RabbitMQ

**Artifacts**: `docs/design/eventstorming/expense-iteration-6.md` + 3 PlantUML diagrams

**Why:** Iteration 6 planning requires tactical DDD modeling before TDD implementation.

**How to apply:** Use these design decisions when implementing the 4 features. ADR-0015 and ADR-0016 still need to be written.
