# User Journey Maps: Shopping List + Email Notification — Iteration 8

**Version**: Iteration 8 design (v0.9.0-SNAPSHOT)
**Status**: Design specification — no code written
**Features covered**: Shopping List (US-TRIPS-050–054), Email Invitation Notification (US-IAM-050)

---

## Feature 1: Shopping List

### Domain Lifecycle Reminder

```
ShoppingItem:

OPEN ──[any participant assigns]──► ASSIGNED ──[assignee marks done]──► PURCHASED
  ▲                                      │
  └────────[assignee unassigns]──────────┘
```

Recipe items are generated from the meal plan. Manual items are added by any participant. Only manual OPEN items can be deleted. PURCHASED is a terminal state.

---

## Journey 1: Organizer — Build and Review List

**Persona**: Organizer (Anna, role: organizer)
**Goal**: Prepare a complete shopping list before the trip starts
**Trigger**: Meal plan has been assigned with recipes; trip is in PLANNING or CONFIRMED state

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Discover | Opens trip detail page | Trip detail shows "Einkaufsliste" section with "Einkaufsliste anzeigen" link | Trip detail — Einkaufsliste article | Organised | Section only appears when meal plan exists — if no meal plan, no entry point | Consider showing a "Erst Essensplan erstellen" hint if no meal plan |
| 2. Open list | Clicks "Einkaufsliste anzeigen" | Shopping list page loads — two sections: recipe items, manual items | `/trips/{id}/shoppinglist` | Satisfied | Initial page may be empty if no recipes assigned yet | Show "Kein Rezept zugewiesen" empty state with link to meal plan |
| 3. Review recipe items | Scans generated ingredient list | 12 items aggregated from 7 recipes, quantities scaled for 4 people | Recipe items section | Impressed | No guarantee recipe ingredients are complete (organizer trusts recipe data) | Show scaling info: "4 Personen · Rezepte für je 2 Portionen" |
| 4. Add missing items | Types name, quantity, unit into inline form, submits | New item appears in manual items section via HTMX update | AddManualItemForm (desktop) | Efficient | Unit field is free text — no validation on unit format | Future: unit autocomplete |
| 5. Adjust after plan change | Clicks "Aktualisieren" | Recipe items regenerated; ASSIGNED/PURCHASED items preserved | Recipe items section header | Reassured | Stale items are non-obvious until user notices discrepancy | Future: highlight "Essensplan zuletzt geändert" timestamp |
| 6. Final review | Applies "Alle" filter | Sees full list including purchased items from earlier preparation | Full list | Ready | Long list with many items — hard to see overall progress | Status filter tabs make progress visible |

### Key Metric
- Time from "Einkaufsliste anzeigen" to first manual item added: target < 30 seconds on desktop.

---

## Journey 2: Participant — Shop at the Store

**Persona**: Participant (Max, role: participant)
**Goal**: Pick up assigned items at the grocery store and mark them as done
**Trigger**: Trip is IN_PROGRESS; organizer has shared the shopping list; Max is at the supermarket with his phone

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Access | Opens Travelmate on phone, navigates to trip, taps "Einkaufsliste anzeigen" | Shopping list opens on mobile — card layout, items grouped by status | `/trips/{id}/shoppinglist` (mobile) | Focused | Navigation to shopping list requires 3 taps from home — too many | Bookmark / PWA shortcut recommendation |
| 2. Find open items | Taps "Ausstehend" filter tab | List filters to OPEN items only — fewer items, easier to scan | Mobile card list, filtered | Relieved | Default shows all items including done — confusing in store | Default to "Ausstehend" filter when arriving from trip detail on mobile |
| 3. Claim item | Taps "Ich übernehme" on "Haferflocken" card | Button disappears, card updates to show "Du · Übernommen" with [Abgeben] and [Erledigt] buttons | Item card — ASSIGNED state | Confident | Tap target for "Ich übernehme" must be large enough for one-handed shopping | Button spans full card width on mobile |
| 4. Pick up item | Picks up oats, taps "Erledigt" | Card updates to show strikethrough "Haferflocken" with "Erledigt" badge | Item card — PURCHASED state | Satisfied | Accidental tap risk for "Erledigt" near "Abgeben" button | Confirmation not needed — PURCHASED is not irreversible from UX perspective; UI shows clear visual change |
| 5. See others' progress | Polls update arrives (5s) | Another participant's item (Milch) changes from OPEN to ASSIGNED by Anna | Item card — ASSIGNED by another | Informed | No push notification — must have page open to see updates | Polling covers the store scenario adequately; push not needed |
| 6. Finish aisle | All "Ausstehend" items purchased | Filtered list shows empty state: "Keine ausstehenden Einträge." | Empty state in filtered view | Accomplished | Unclear if list is truly complete or filter is just hiding items | "Alle (12) · Ausstehend (0)" summary at top |

### Key Metric
- Time to claim + mark first item: target < 10 seconds on mobile from list open.
- Claim action requires maximum 1 tap; purchase requires maximum 1 additional tap.

### Mobile UX Design Decisions

**Default filter on mobile:** The server detects the device type via `User-Agent` or screen width hint in query parameter and defaults to `?filter=open` when accessed from the trip detail page on mobile. This is a server-side decision — no client JS required. The trip detail link can pass `?filter=open` directly for mobile participants.

**Full-width buttons:** On mobile cards, action buttons use `width: 100%` — large tap targets reduce misclicks when one hand holds groceries.

**Polling while active:** HTMX polling (`every 5s`) is active while the page is open. This is appropriate for the store scenario — concurrent participants need live visibility. The polling is silent (no spinner), so it does not interrupt the user.

---

## Journey 3: Multi-User Concurrent Shopping

**Persona**: Organizer (Anna) + Participant (Max) shopping simultaneously in different aisles
**Goal**: Divide shopping efficiently without duplicate purchases
**Trigger**: Both are at the store; Max focuses on dry goods, Anna on produce

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Coordination | Both open shopping list on phones | Both see same OPEN items | `/trips/{id}/shoppinglist` (both) | Ready | No way to pre-assign items before arriving — first-come first-served | Future: pre-assign items on desktop before shopping day |
| 2. Max claims dry goods | Max claims "Haferflocken", "Nudeln" | Both items show "Max — Übernommen" within 5s on Anna's phone | Max's phone: ASSIGNED; Anna's phone: polls update | Coordinated | 5s polling delay means brief window where both could try to claim same item | Optimistic locking: if two users simultaneously claim same item, second gets 409 → system auto-shows "Bereits übernommen" |
| 3. Race condition | Both tap "Ich übernehme" on "Milch" at the same time | Server accepts first request; second returns 409. Both UIs reflect the winner. | Conflict resolution | Minimal friction | The "loser" of the race sees their button fail — confusing without explanation | Return inline error: "Bereits von [Name] übernommen." The full row is returned in ASSIGNED state showing the winner's name |
| 4. Anna adjusts | Anna sees Milch now shows "Max — Übernommen" | Anna skips Milch, claims "Tomaten" instead | Anna's phone | Flexible | No reassignment by organiser (only assignee can unassign) | Organizer could be given override power — deferred to future iteration |
| 5. Completion | All items marked | Both see "Erledigt" filter with full list crossed out | Completed shopping list | Accomplished | No celebration state — list just goes empty | Future: summary "Einkauf abgeschlossen — 15 Einträge gekauft" |

### Conflict Resolution Detail

The assign endpoint uses an optimistic lock: the HTMX request includes the current item status as a form field (`status=OPEN`). The server rejects the request with 409 if the item is no longer OPEN. The response body contains the updated item row fragment (in ASSIGNED state), which replaces the row via `hx-swap="outerHTML"`. The user sees the item as already claimed — no alert, no blocking, just the current truth.

---

## Journey 4: Organizer — Post-Trip Review

**Persona**: Organizer (Anna)
**Goal**: Verify the shopping was complete after returning from the trip
**Trigger**: Trip has ended; Anna wants to cross-check that all items were purchased

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Access | Opens shopping list from trip detail | Shopping list opens | `/trips/{id}/shoppinglist` | Reflective | List is still available on COMPLETED trips | List is read-only if trip is COMPLETED — no further mutations |
| 2. Review status | Applies "Ausstehend" filter | Shows any items never purchased | Filtered list or empty state | Curious | Items left OPEN could indicate forgotten items | Show count: "2 Einträge nicht erledigt" |
| 3. Archive | No archive action needed | Shopping list persists with trip | — | Neutral | List stays indefinitely — no cleanup needed for now | Future: export list as PDF or text |

---

## Feature 2: Email Invitation Notification (US-IAM-050)

### Context

Currently, trip invitations are stored in the database but no email is sent to invited participants. The IAM SCS already has a `InvitationEmailListener` and Spring Mail configured (used for Keycloak external invitations). US-IAM-050 extends this to send an HTML invitation email to all invited participants.

---

## Journey 5: Participant Receives Trip Invitation Email

**Persona**: New participant (Lisa), invited by organizer (Anna) via the "Mitglied einladen" dialog
**Goal**: Respond to a trip invitation and join the trip
**Trigger**: Anna has invited Lisa to "Alpen-Urlaub 2026" from the trip detail page

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Email arrives | Lisa opens email client | Clean HTML email: "Anna Müller hat dich zur Reise eingeladen" | Inbox — email client (mobile or desktop) | Surprised, Curious | Email may land in spam if domain not configured (SPF/DKIM) | Ensure Mailpit in dev, production mail provider configured |
| 2. Read email | Lisa reads trip details | Trip name, date range clearly visible in info box | Email body | Interested | Date format must match locale — "12.07.2026" for DE, "Jul 12, 2026" for EN | Email locale matches invitation language |
| 3. Tap CTA | Lisa taps "Einladung annehmen" | Opens browser to `/trips/accept/{token}` | CTA button | Engaged | Must be logged in to accept — redirected to Keycloak login first | After login, redirect back to acceptance URL |
| 4. Accept | Lisa sees acceptance confirmation | Trip appears in Lisa's trip list | Trip list page | Satisfied | Flow requires login which adds friction for returning users | Keycloak remember-me session reduces friction |
| 5. Ignore | Lisa does nothing for 7 days | Invitation expires | — | Neutral | No reminder email (out of scope for this iteration) | Future: reminder email after 3 days |

### Email Design Decisions

**Why HTML email over plain text:** The trip info box (name + date range) is most legible as a distinct styled block. Plain text cannot provide this visual hierarchy. HTML email with inline CSS is the correct approach for travel context.

**Branding:** PicoCSS blue (`#2563eb`) is the primary action colour for the CTA button, consistent with the web app. The email does not attempt to fully match the PicoCSS design system — it uses a simplified inline-CSS version that is stable across email clients.

**Expiry disclosure:** The 7-day expiry is shown clearly in the email to set expectations. This prevents confusion when users try to accept a stale link.

**Accept vs. Decline in email:** The email contains only an "Annehmen" CTA. "Ablehnen" (Decline) is available after clicking through to the web app, on the trip detail or invitation confirmation page. Including both buttons in the email would complicate the layout and reduce visual hierarchy of the primary CTA.

**Text fallback (plain-text alternative):** The Spring Mail `MimeMessageHelper` should include a plain-text alternative:

```
Hallo [Name],

[Einladender] hat dich zur Reise "[Trip name]" eingeladen.
Zeitraum: [Startdatum] bis [Enddatum]

Einladung annehmen: [URL]

Diese Einladung läuft in 7 Tagen ab.

Travelmate — Gemeinsam planen.
```

---

## Journey 6: External User Receives Registration + Invitation Email

**Persona**: New external user (Klaus), invited via "Per E-Mail einladen" — no Travelmate account yet
**Goal**: Register and join the trip
**Trigger**: Anna has invited Klaus by email from the external invite dialog

This journey differs from Journey 5: Klaus does not have an account yet. The IAM SCS creates a Keycloak user + Account when the `ExternalUserInvitedToTrip` event is consumed, then sends a registration + invitation email.

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Email arrives | Klaus opens email | Email variant: "Du wurdest zu Travelmate eingeladen und zur Reise [Name] hinzugefügt. Setze dein Passwort, um anzufangen." | Inbox | Confused (unfamiliar platform) | Klaus has never heard of Travelmate | Email explains what Travelmate is in one sentence |
| 2. Set password | Klaus clicks "Passwort setzen" | Keycloak password reset flow (already in place) | Keycloak password-reset page | Compliant | Keycloak page is not branded like Travelmate | Keycloak realm branding (out of scope) |
| 3. Log in | Klaus logs in | Redirected to trip detail — already a participant (auto-joined) | Trip detail page | Delighted | The trip is already there — no further acceptance step needed | Show welcome banner: "Willkommen bei Travelmate! Du bist jetzt Teil von [trip name]." |

### Email variant for external users

The external invitation email template (`email/external-invitation.html`) differs from the member invitation email:

- Headline: "Du wurdest zu Travelmate eingeladen" (not just to a trip)
- Body: brief one-sentence explanation of Travelmate ("Travelmate hilft Reisegruppen beim gemeinsamen Planen.")
- CTA: "Passwort setzen und loslegen" (links to Keycloak password reset URL)
- Trip info box: same as standard invitation email
- No accept/decline flow needed — account is already created and participant is auto-joined

---

## Summary: Key UX Principles for Iteration 8

### Shopping List
1. **Mobile-first for consumption**: Participants use the list at the store. Every interaction (claim, done) must be achievable with one thumb.
2. **Live without push**: HTMX polling every 5s is sufficient for coordination in a store. Silent, no spinner.
3. **Scaling is opaque but present**: Users see scaled quantities with a muted explanation. They don't need to understand the algorithm.
4. **Status filter is essential**: "Ausstehend" is the default view when shopping. "Alle" is the default when planning.
5. **Optimistic UI, pessimistic conflict resolution**: Claim immediately on tap; show conflict in place without blocking the user.

### Email Notification
1. **Clarity over style**: The email's job is to communicate trip name, dates, inviter, and CTA. Minimalist design with a single blue button.
2. **Locale-aware**: Date format and language match the recipient's Travelmate locale.
3. **Always include a text link**: CTA buttons fail in some clients and spam filters. Plain fallback link is mandatory.
4. **External users need a different email**: "Set your password" is a fundamentally different ask than "Accept invitation". Two templates.
