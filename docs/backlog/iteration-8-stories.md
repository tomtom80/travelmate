# Iteration 8 — Refined User Stories: Shopping List + Email Notifications

**Date**: 2026-03-17
**Target Version**: v0.9.0-SNAPSHOT
**Bounded Contexts**: Trips (Shopping List), Trips/IAM (Email Notifications)

---

## Overview

This document provides detailed acceptance criteria, edge cases, and implementation guidance for
the six user stories in Iteration 8. The Shopping List stories (S8-A through S8-E) are all within
the Trips bounded context. The email notification story (S8-F) extends the existing
`InvitationEmailListener` infrastructure already present in Trips.

---

## Dependencies Between Stories

```
S8-A (Auto-Generate Shopping List)
  └─► S8-B (Add Manual Item)       — both require ShoppingList aggregate to exist
  └─► S8-C (Assign Item)           — requires items to exist
        └─► S8-D (Mark Purchased)  — requires assignment to exist
              └─► S8-E (Real-Time Updates) — requires full item lifecycle to be stable

S8-F (Email Notification for Trip Invitation)
  — independent, but builds on existing InvitationEmailListener infrastructure
```

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S8-A | Core aggregate; all other shopping list stories depend on it |
| 2 | S8-B | Adds manual items; completes the "add item" surface area before claiming |
| 3 | S8-C | Assignment requires items; unblock participant workflow |
| 4 | S8-D | Purchase requires assignment; completes the item lifecycle |
| 5 | S8-E | Polling/SSE makes sense only once the full lifecycle is testable end-to-end |
| 6 | S8-F | Independent of shopping list; can be implemented in parallel after S8-A |

---

## Domain Model Reference

### ShoppingList Aggregate (new)

```
ShoppingList
  ShoppingListId  (UUID)
  TenantId        (scoping — CRITICAL for cross-tenant isolation)
  TripId
  List<ShoppingItem>

ShoppingItem
  ShoppingItemId  (UUID)
  name            (String, 1–100 chars)
  quantity        (BigDecimal, > 0)
  unit            (String, 1–20 chars, e.g. "g", "ml", "Stück", "Pack")
  source          (RECIPE | MANUAL)
  status          (OPEN | ASSIGNED | PURCHASED)
  assignedTo      (UUID memberId, nullable)
  assigneeName    (String, denormalized for display, nullable)
```

### Scaling Rule

`scaledQuantity = ingredientQuantity × (participantCount / recipeServings)`

Participant count is the number of confirmed participants on the Trip
(`trip.participants().size()`). Rounding: `RoundingMode.HALF_UP` to 2 decimal places.
The display layer may further round to sensible precision (e.g., whole grams).

### Aggregation Rule

Two ingredients are aggregated (summed) if and only if they share the same
`name` (case-insensitive trim) **and** the same `unit` (case-insensitive trim).
If the same ingredient appears in different units (e.g., "500g flour" and "2 cups flour"),
they are kept as separate items — no unit conversion is performed.

---

## Story S8-A: US-TRIPS-050 — Auto-Generate Shopping List from Meal Plan

**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** the Shopping List to be automatically generated from the Meal
Plan's recipes, **so that** I don't have to compute ingredients manually.

### Acceptance Criteria

#### Happy Path — Generation

- **Given** a Trip has a MealPlan with at least one PLANNED slot that has a Recipe assigned,
  **When** I navigate to the Shopping List page,
  **Then** the list shows one item per aggregated ingredient, with quantity scaled by
  `participantCount / recipeServings`, source=RECIPE, and status=OPEN.

- **Given** two PLANNED MealSlots both reference Recipes containing "Flour" with unit "g"
  (e.g. 300g and 200g, both already scaled),
  **When** the Shopping List is generated,
  **Then** a single item "Flour — 500g" appears (aggregated sum).

- **Given** two PLANNED MealSlots reference Recipes containing "Salt" in "g" and "Salt" in "tsp"
  respectively,
  **When** the Shopping List is generated,
  **Then** two separate items appear: one "Salt (g)" and one "Salt (tsp)" — no unit conversion.

#### Scaling

- **Given** a Recipe has servings=4 and the Trip has 6 participants,
  **When** the Shopping List is generated,
  **Then** each ingredient quantity is multiplied by 6/4 = 1.5, rounded to 2 decimal places
  using `HALF_UP`.

- **Given** a Recipe has servings=4 and the Trip has 3 participants,
  **When** the Shopping List is generated,
  **Then** each ingredient quantity is multiplied by 3/4 = 0.75, rounded to 2 decimal places.
  The resulting value (e.g., 0.75 packs) is stored and displayed as-is; no minimum-of-1 rule
  is applied at the domain level.

- **Given** a Recipe has servings=0 (invalid data edge case),
  **When** Shopping List generation is triggered,
  **Then** that recipe's ingredients are excluded from generation and a warning is logged.
  (Servings is validated > 0 on creation; this case guards against corrupt data.)

#### Excluded Slots

- **Given** a MealSlot has status=SKIP,
  **When** the Shopping List is generated,
  **Then** that slot's Recipe ingredients are excluded, even if a Recipe is assigned.

- **Given** a MealSlot has status=EATING_OUT,
  **When** the Shopping List is generated,
  **Then** that slot's Recipe ingredients are excluded.

- **Given** a MealSlot has status=PLANNED but no Recipe assigned,
  **When** the Shopping List is generated,
  **Then** no ingredients are contributed by that slot (slot is simply ignored).

#### Empty States

- **Given** a Trip has no MealPlan yet,
  **When** I navigate to the Shopping List page,
  **Then** I see an empty list with the message "No meal plan exists for this trip yet. Create a
  meal plan first to auto-generate shopping items."
  The page still allows adding manual items (S8-B).

- **Given** a Trip has a MealPlan but no Recipes are assigned to any PLANNED slot,
  **When** I navigate to the Shopping List page,
  **Then** I see an empty list with the message "No recipes are assigned in the meal plan yet.
  Assign recipes to meal slots to auto-generate shopping items."
  The page still allows adding manual items.

#### Regeneration

- **Given** a Shopping List has been generated,
  **When** the Organizer assigns a Recipe to a previously recipe-less MealSlot,
  **Then** the Shopping List is regenerated: RECIPE items are recomputed from scratch,
  MANUAL items are preserved unchanged, and items that were ASSIGNED or PURCHASED retain their
  assignment/status only if the same item name+unit still appears in the newly generated set.
  Items that no longer exist after regeneration are removed regardless of their status.

  > Rationale: A participant who already purchased an item that the Organizer subsequently
  > removes from the meal plan should not see a ghost "PURCHASED" item.

- **Given** a Shopping List has been generated,
  **When** the Organizer removes a Recipe from a MealSlot (clears assignment),
  **Then** the Shopping List is regenerated using the same rules above.

- **Given** a Shopping List has been generated,
  **When** the Organizer changes a MealSlot status to SKIP or EATING_OUT,
  **Then** the Shopping List is regenerated using the same rules above.

  > Implementation note: Regeneration is triggered by the application service after any
  > `MealPlan` mutation (slot status change, recipe assignment/removal). This is an explicit
  > service-layer call, not an event-driven side effect, to keep the Shopping List consistent
  > within the same transaction.

#### TripStatus Constraints

- **Given** a Trip is in any status (PLANNING, CONFIRMED, IN_PROGRESS),
  **When** I access the Shopping List,
  **Then** I can view and interact with it.

- **Given** a Trip is COMPLETED or CANCELLED,
  **When** I access the Shopping List,
  **Then** I can view the list in read-only mode. No new items can be added and no status
  transitions are possible.

#### Manual Quantity Adjustment

- Out of scope for this story. Quantity adjustment of RECIPE items is not supported in
  Iteration 8. The Organizer can override quantities by editing the underlying Recipe instead.
  (See Out-of-Scope section.)

#### Multi-Tenancy

- **Given** two Tenants each have a Trip with a Shopping List,
  **When** one Tenant views their Shopping List,
  **Then** only items scoped to their TenantId are visible.
  Cross-tenant data access is a security violation and must be enforced at the repository layer.

### Technical Notes

- Bounded Context: Trips
- New Aggregate: `ShoppingList` (ShoppingListId, TenantId, TripId, List<ShoppingItem>)
- New Repository port: `ShoppingListRepository` (in `domain/shoppinglist/`)
- Application service: `ShoppingListService.generateFromMealPlan(tripId, tenantId)`
- Regeneration method: `ShoppingListService.regenerate(tripId, tenantId)` — replaces all
  RECIPE items, preserves MANUAL items
- Flyway migration: V10 — `shopping_list`, `shopping_item` tables
- UI: `/trips/{tripId}/shopping-list` tab on Trip detail page
- Domain Events produced: none (Shopping List is a derived artifact)
- Domain Events consumed: none (service is called directly by MealPlan mutations)

---

## Story S8-B: US-TRIPS-051 — Add Manual Shopping Item

**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to manually add items to the Shopping List (snacks, drinks,
household items), **so that** everything we need is in one place.

### Acceptance Criteria

#### Authorization

- **Given** I am any authenticated Participant of the Trip (organizer or participant role),
  **When** I submit a new manual item,
  **Then** the item is added. Both roles can add manual items.

- **Given** I am not a Participant of the Trip,
  **When** I attempt to add a manual item via POST,
  **Then** I receive HTTP 403 Forbidden.

#### Happy Path

- **Given** I am on the Shopping List page for a Trip,
  **When** I enter a name, quantity, and unit and click "Add",
  **Then** a new ShoppingItem is created with source=MANUAL, status=OPEN, and is immediately
  visible in the list.

- **Given** no Shopping List exists yet for the Trip (MealPlan not created),
  **When** I add a manual item,
  **Then** a Shopping List is created implicitly for this Trip, and the item is added.

#### Validation

- **Given** I submit a name that is empty or blank,
  **When** the form is submitted,
  **Then** I see a validation error "Item name is required."

- **Given** I submit a name longer than 100 characters,
  **When** the form is submitted,
  **Then** I see a validation error "Item name must not exceed 100 characters."

- **Given** I submit a quantity of 0 or negative,
  **When** the form is submitted,
  **Then** I see a validation error "Quantity must be greater than 0."

- **Given** I submit a unit that is empty or blank,
  **When** the form is submitted,
  **Then** I see a validation error "Unit is required."

- **Given** I submit a unit longer than 20 characters,
  **When** the form is submitted,
  **Then** I see a validation error "Unit must not exceed 20 characters."

#### Edit and Delete

- **Given** a MANUAL item exists with status=OPEN,
  **When** the creator (or Organizer) edits its name, quantity, or unit and saves,
  **Then** the item is updated.

- **Given** a MANUAL item exists,
  **When** the Organizer deletes it,
  **Then** it is permanently removed regardless of its current status.

- **Given** a MANUAL item is ASSIGNED or PURCHASED,
  **When** any Participant (not only assignee) tries to delete it,
  **Then** only the Organizer can delete it; regular Participants can only delete their own
  OPEN items. (This prevents accidental deletion of in-flight purchases.)

  > Simplification for Iteration 8: only Organizers can delete shopping items in any status.
  > Participants can add but cannot delete.

#### Preservation Across Regeneration

- **Given** a Shopping List has MANUAL items,
  **When** the Shopping List is regenerated due to a MealPlan change,
  **Then** all MANUAL items are preserved exactly as they were (name, quantity, unit, status,
  assignedTo).

#### Duplicate Handling

- **Given** I add a manual item with the same name and unit as an existing RECIPE item,
  **When** the item is saved,
  **Then** a new separate MANUAL item is created. No automatic merging with RECIPE items occurs.

  > Rationale: a participant may consciously want to note they are bringing an extra pack of
  > something already on the recipe-generated list.

### Technical Notes

- Bounded Context: Trips
- Command: `AddManualShoppingItemCommand(tripId, tenantId, name, quantity, unit)`
- Value Objects: validate in compact constructors via `Assertion`
- UI: Inline form on Shopping List page, HTMX `hx-post` to append item row
- Authorization: both `organizer` and `participant` Keycloak roles are allowed to add

---

## Story S8-C: US-TRIPS-052 — Assign Shopping Item to Myself

**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to assign a Shopping Item to myself, **so that** others know I
will take care of buying it.

### Acceptance Criteria

#### Happy Path

- **Given** a ShoppingItem is OPEN,
  **When** I click "I'll get it",
  **Then** the item transitions to ASSIGNED, `assignedTo` is set to my memberId,
  `assigneeName` is set to my display name, and all other Participants see my name next to
  the item.

- **Given** a ShoppingItem is ASSIGNED to me,
  **When** I click "Unassign",
  **Then** the item transitions back to OPEN, `assignedTo` is cleared, and `assigneeName`
  is cleared.

#### Assignment Conflicts

- **Given** a ShoppingItem is already ASSIGNED to another Participant,
  **When** I try to click "I'll get it",
  **Then** the button is disabled (or hidden) and I see the assignee's name. I cannot
  override another Participant's assignment.

- **Given** a ShoppingItem is already ASSIGNED to another Participant,
  **When** the Organizer clicks "Unassign" (via an organizer-only control),
  **Then** the item transitions back to OPEN, clearing the assignment.

  > Rationale: the Organizer may need to reassign items if a participant drops out of a trip
  > or is no longer available to shop.

- **Given** a ShoppingItem is PURCHASED,
  **When** I view the Shopping List,
  **Then** there is no "I'll get it" or "Unassign" option. The item is read-only in this
  regard.

#### Applies to Both Sources

- **Given** a ShoppingItem has source=RECIPE,
  **When** I assign it to myself,
  **Then** the same OPEN → ASSIGNED transition applies as for MANUAL items.

- **Given** a ShoppingItem has source=MANUAL,
  **When** I assign it to myself,
  **Then** the same OPEN → ASSIGNED transition applies.

#### Concurrent Assignment Race Condition

- **Given** two Participants click "I'll get it" for the same OPEN item at the same moment,
  **When** both requests are processed,
  **Then** only the first writer wins (optimistic locking or pessimistic DB lock).
  The second request receives a conflict response and the UI shows "This item was just
  claimed by [Name]." The item remains ASSIGNED to the first claimant.

  > Implementation: use `@Version` on the `shopping_item` row for optimistic locking,
  > or a single UPDATE with a `WHERE status = 'OPEN'` predicate that returns 0 rows on
  > conflict.

### Technical Notes

- Bounded Context: Trips
- Commands: `AssignShoppingItemCommand(tripId, tenantId, itemId, memberId, memberName)`,
  `UnassignShoppingItemCommand(tripId, tenantId, itemId, requestingMemberId)`
- Status transition: `OPEN → ASSIGNED` (assign), `ASSIGNED → OPEN` (unassign)
- Transition is only valid when `assignedTo == requestingMemberId` for unassign
  (or requestingMember has `organizer` role)
- UI: HTMX `hx-post` for assign/unassign, swap the item row fragment
- Domain Events: none

---

## Story S8-D: US-TRIPS-053 — Mark Shopping Item as Purchased

**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to mark a Shopping Item as purchased, **so that** others know
it's done.

### Acceptance Criteria

#### Standard Flow

- **Given** a ShoppingItem is ASSIGNED to me,
  **When** I click "Done" (Gekauft),
  **Then** the item transitions to PURCHASED and is visually distinguished (strikethrough or
  moved to a "Completed" section).

- **Given** a ShoppingItem is PURCHASED,
  **When** all Participants view the Shopping List,
  **Then** they all see the item as PURCHASED with the assignee's name.

#### Who Can Mark as Purchased

- **Given** a ShoppingItem is ASSIGNED to me,
  **When** I mark it as purchased,
  **Then** the transition succeeds.

- **Given** a ShoppingItem is ASSIGNED to another Participant,
  **When** I (a different Participant) try to mark it as purchased,
  **Then** the action is rejected with the message "Only the assignee can mark this item as
  purchased." The Organizer is exempt from this rule and can mark any ASSIGNED item.

#### Reverting a Purchase (Un-purchase)

- **Given** a ShoppingItem is PURCHASED,
  **When** the assignee or Organizer clicks "Undo" (Rückgängig),
  **Then** the item transitions back to ASSIGNED (with the original assignee preserved).

  > Rationale: accidental taps on mobile are common. Un-purchase should revert to ASSIGNED
  > (not OPEN) because the person is presumably still planning to buy it.

#### Direct Purchase Without Assignment

- **Given** a ShoppingItem is OPEN (not yet assigned),
  **When** a Participant clicks "Mark as purchased",
  **Then** this is allowed as a shortcut: the item is first implicitly assigned to the
  requesting Participant (OPEN → ASSIGNED), then immediately transitioned to PURCHASED.
  The assignee is recorded.

  > Rationale: at the supermarket, a participant may simply grab something and mark it done
  > without the two-step flow.

#### Completed Trip Constraint

- **Given** a Trip is COMPLETED or CANCELLED,
  **When** I try to mark an item as purchased (or undo a purchase),
  **Then** the action is rejected. The Shopping List is read-only for completed/cancelled
  trips.

### Technical Notes

- Bounded Context: Trips
- Commands: `MarkShoppingItemPurchasedCommand(tripId, tenantId, itemId, requestingMemberId)`,
  `UnmarkShoppingItemPurchasedCommand(tripId, tenantId, itemId, requestingMemberId)`
- Status transitions:
  - `ASSIGNED → PURCHASED` (mark)
  - `OPEN → ASSIGNED → PURCHASED` (direct purchase shortcut)
  - `PURCHASED → ASSIGNED` (undo)
- UI: "Done" button visible only for the assignee (and Organizer); "Undo" button on
  PURCHASED items
- Domain Events: none

---

## Story S8-E: US-TRIPS-054 — Real-Time Shopping List Updates

**Epic**: E-TRIPS-06
**Priority**: Should
**Size**: M
**As a** Participant, **I want** to see Shopping List changes made by others in near-real-time,
**so that** we don't buy the same things.

### Acceptance Criteria

#### Polling Requirement

- **Given** two Participants have the Shopping List open simultaneously,
  **When** Participant A assigns or purchases an item,
  **Then** Participant B's view reflects the change within a few seconds without a manual
  page refresh.

- **Given** the Organizer regenerates the Shopping List (by changing the MealPlan),
  **When** Participants are viewing the Shopping List,
  **Then** they see the updated list (added/removed items, changed quantities) within a few
  seconds.

#### Implementation: HTMX Polling

- **Given** the Shopping List page is loaded,
  **Then** the item list fragment uses `hx-get` + `hx-trigger="every 5s"` to refresh the
  list partial.

  > Polling is preferred over SSE for Iteration 8 because:
  > (a) the Trips SCS is a standard servlet (blocking) Spring Boot app — SSE on servlet
  > requires careful thread management.
  > (b) Polling at 5 seconds is acceptable for a shopping coordination use case.
  > SSE is recorded as a future improvement.

#### Conflict Handling (Concurrent Assignment)

- **Given** two Participants attempt to assign the same OPEN item simultaneously,
  **When** the second request arrives after the first has already been committed,
  **Then** the second request is rejected (see S8-C conflict handling).
  The next polling cycle will show the first claimant's name to the second Participant.

#### Bandwidth and Battery Considerations

- **Given** the Shopping List page is open,
  **When** the Trip is in COMPLETED or CANCELLED status,
  **Then** polling is disabled (no `hx-trigger` attribute rendered). The list is static.

- **Given** the browser tab becomes inactive (Page Visibility API),
  **Then** out-of-scope for Iteration 8; HTMX polling continues regardless.
  This is a known trade-off accepted for this iteration.

#### Partial vs. Full List Refresh

- The polling refresh replaces the entire shopping item list fragment
  (`<div id="shopping-list-items">`), not individual rows.
  This simplifies the implementation at the cost of a slightly higher payload.
  Per-row delta updates are out of scope for Iteration 8.

### Technical Notes

- Bounded Context: Trips
- UI: `hx-get="/trips/{tripId}/shopping-list/items"` +
  `hx-trigger="every 8s"` on the item list container
- New controller endpoint: `GET /trips/{tripId}/shopping-list/items` returns the
  `shopping-list :: #shopping-list-items` fragment
- No new domain objects required; this is a pure UI concern
- Future improvement: replace polling with SSE (`SseEmitter`) in a later iteration

---

## Story S8-F: US-IAM-050 — Email Notification for Trip Invitation

**Epic**: E-IAM-06
**Priority**: Must
**Size**: M
**As a** Travel Party being invited to a Trip, **I want** to receive an email notification,
**so that** I am aware of the invitation and can respond.

### Acceptance Criteria

#### Email Delivery

- **Given** an Organizer creates a Trip Invitation for a Member of another Travel Party
  (or their own Travel Party),
  **When** the Invitation is persisted,
  **Then** the invitee receives an email within a few seconds.

- **Given** the InvitationCreated domain event is published after commit,
  **When** `InvitationEmailListener.onInvitationCreated` processes it,
  **Then** exactly one email is sent to `event.inviteeEmail()`.

  > Note: the `InvitationEmailListener` already exists (introduced in Iteration 3 for the
  > external invitation flow). This story extends its scope to cover standard Member
  > invitations as well. Review whether `InvitationCreated` is published for MEMBER-type
  > invitations and add it if missing.

#### Email Content

- **Given** an invitation email is sent,
  **When** the recipient opens it,
  **Then** the email contains:
  - Subject: "Einladung zur Reise: {tripName}" (German default; consider i18n in a future
    story)
  - Salutation with the invitee's first name
  - The name of the Trip
  - The Trip start and end dates
  - The inviter's full name ("Invited by {firstName} {lastName}")
  - A deep link to the Invitation view:
    `{gateway-base-url}/trips/invitations/{invitationId}`
  - A note that the link requires login

- **Given** the email template is `email/invitation-member.html` (Thymeleaf),
  **When** variables are bound,
  **Then** the template receives: `inviteeFirstName`, `tripName`, `tripStartDate`,
  `tripEndDate`, `inviterFirstName`, `inviterLastName`, `invitationLink`.

  > `invitationLink` is a new variable to be added to `InvitationCreated` or constructed
  > in the listener from a configurable `travelmate.base-url` property.

#### Deep Link

- **Given** the invitee clicks the link in the email,
  **When** they are not yet logged in,
  **Then** the Gateway OIDC flow intercepts, the user logs in, and is redirected to
  `/trips/invitations/{invitationId}` where they can accept or decline.

- **Given** the invitee clicks the link,
  **When** they are already logged in,
  **Then** they are taken directly to the Invitation detail view.

#### Duplicate Invitation Prevention

- **Given** an Organizer has already invited a Member (Invitation exists),
  **When** the Organizer tries to invite the same Member again,
  **Then** the second Invitation is rejected at the domain level (existing behavior from
  US-TRIPS-010). No duplicate email is sent because no duplicate Invitation is created.

#### Email Infrastructure

- **Given** the SMTP server is unavailable when an invitation email is sent,
  **When** the `InvitationEmailListener` catches the `MessagingException`,
  **Then** the error is logged at ERROR level with the invitee's email and trip name.
  The Invitation is **not** rolled back (the invitation was already committed before the
  `@TransactionalEventListener(phase = AFTER_COMMIT)` runs).

  > Current behaviour in `InvitationEmailListener` already follows this pattern: it catches
  > and logs exceptions without rethrowing. This is acceptable for Iteration 8. A DLQ-based
  > retry for email delivery is a future enhancement.

- **Given** the application is running under `@Profile("test")`,
  **Then** `InvitationEmailListener` is not active (it is annotated `@Profile("!test")`).
  No emails are sent in tests.

#### Member Has No Email (Invariant)

This case cannot occur: an Account (Member) is always created with an email address that is
also their Keycloak login. The `AccountRegistered` event carries the email. The
`InvitationCreated` event receives the email from the TravelParty projection.
No defensive handling is needed beyond the existing null-safety in `Assertion`.

#### Which SCS Sends the Email

The email is sent by the **Trips SCS** via `InvitationEmailListener` in
`adapters/mail/`. This is consistent with the existing external invitation email flow.
The IAM SCS is not involved in Trip invitation emails.

### Technical Notes

- Bounded Context: Trips
- Existing infrastructure: `InvitationEmailListener` + `InvitationCreated` event + Mailpit
  dev SMTP (port 1025)
- Action required: verify that `InvitationCreated` is published for standard MEMBER-type
  invitations (not only EXTERNAL). Extend `InvitationService.invite()` if not.
- New `InvitationCreated` field: `invitationLink` (String) or construct URL in listener
  from `@Value("${travelmate.base-url}")` config property.
- Template: extend `email/invitation-member.html` to include the deep link anchor.
- Dev verification: open Mailpit at `http://localhost:8025` after inviting a member and
  confirm delivery.
- E2E test: invite a member and assert via Mailpit REST API that one email was delivered
  to the correct address with the correct subject.

---

## Out-of-Scope for Iteration 8

The following items are explicitly excluded from this iteration:

| Item | Reason |
|------|--------|
| Manual quantity adjustment of RECIPE items on the Shopping List | Organizer edits the Recipe instead; direct override adds complexity to regeneration |
| Per-item delta updates (only the changed row refreshes) | Full list refresh is sufficient; SSE/WebSocket is a future story |
| SSE-based real-time updates | Polling at 8s is acceptable; SSE requires servlet async configuration |
| Bring! app integration (US-TRIPS-055) | API credentials management and sync are out of scope |
| SMS notifications (US-IAM-051) | Requires external SMS gateway; deferred |
| i18n for email subjects/body | German default is sufficient; locale-aware emails are a future story |
| Notification preferences (US-IAM-052) | All members receive all invitation emails; preferences deferred |
| Email retry / DLQ for failed invitation emails | Logged and accepted; retry infrastructure is a separate story |
| Shopping List for COMPLETED trips (write access) | Read-only view is sufficient; retroactive updates are not a use case |
| Shopping List visibility for non-participants | Only Trip participants can view the Shopping List |

---

## Ubiquitous Language Compliance

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| Einkaufsliste | Shopping List | ShoppingList | Trips |
| Einkaufsartikel | Shopping Item | ShoppingItem | Trips |
| Offen | Open | OPEN | Trips — item status |
| Zugewiesen | Assigned | ASSIGNED | Trips — item status |
| Gekauft | Purchased | PURCHASED | Trips — item status |
| Ich kaufe das | I'll get it | assign | Trips — action |
| Gekauft ✓ | Done | markPurchased | Trips — action |
| Rezept-Artikel | Recipe Item | source=RECIPE | Trips — item origin |
| Manueller Artikel | Manual Item | source=MANUAL | Trips — item origin |
| Einladung | Invitation | Invitation | Trips |
| Einladungslink | Invitation Link | invitationLink | Trips — email variable |
