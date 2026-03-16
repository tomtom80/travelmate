# User Journey Maps: Expense SCS — Iteration 6 Features

**Version**: Iteration 6 design (v0.7.0)
**Status**: Design specification — no code written
**Features covered**: Receipt Review (Four-Eyes), Settlement Summary, Expense Categories, Accommodation Cost Entry

---

## Feature 1: Receipt Review Workflow (Four-Eyes)

### New Receipt States

```
DRAFT ──────────────────────────────────────────────────────────────────────────────┐
  │                                                                                  │
  └─ [Participant submits] ──► SUBMITTED ──► [Organizer approves] ──► APPROVED      │
                                   │                                                 │
                                   └──► [Organizer rejects + reason] ──► REJECTED   │
                                                 │                                   │
                                                 └─ [Participant edits + resubmits] ─┘
```

### Journey: Participant Submits a Receipt

**Persona**: Participant (role: participant)
**Goal**: Add a receipt for review so it counts toward the shared expense
**Trigger**: Participant paid for something during the trip

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Initiate | Taps "Beleg hinzufügen" | Opens dialog with form | Expense detail — receipt section | Neutral | Dialog on mobile requires good tap targets | Pre-fill "Bezahlt von" with current user |
| 2. Fill form | Enters description, amount, date, selects category, "Bezahlt von" defaults to self | Live validation on amount | Add receipt dialog | Focused | Category dropdown: too many options to scan | Group categories by frequency of use |
| 3. Submit | Taps submit | HTMX posts form; receipt appears in list with "Eingereicht" badge | Receipt list fragment | Relieved | No confirmation the review process is clear | Show "Ihr Beleg wird geprüft" notice |
| 4. Wait | Returns later | Receipt shows status badge | Receipt list | Uncertain | No push notification — must check manually | Status change is visible on next page load |
| 5. Sees approval | Checks expense page | "Genehmigt" badge | Receipt row | Satisfied | — | Show approval in a distinct visual style |
| 5b. Sees rejection | Checks expense page | "Abgelehnt" badge + reason inline | Receipt row | Frustrated | Reason may be unclear | Editable inline — one tap to open edit dialog |
| 6. Re-submits | Edits rejected receipt, submits again | Status reverts to "Eingereicht" | Edit receipt dialog | Hopeful | Starting from scratch loses original context | Pre-populate edit form with existing values |

### Journey: Organizer Reviews Submitted Receipts

**Persona**: Organizer (role: organizer)
**Goal**: Verify and approve/reject receipts submitted by participants
**Trigger**: One or more receipts are in SUBMITTED state

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Notice | Opens expense detail | Badge count "3 zu prüfen" or highlighted section | Expense detail — receipt section header | Attentive | No indication from outside the page | Badge on trip list / trip detail expense card |
| 2. Scan | Scrolls review queue | Submitted receipts grouped at top, details visible | Receipt review section | Focused | Long list with many receipts is hard to scan | Show submitter name prominently |
| 3. Examine | Reads receipt details | Full receipt info: description, amount, category, date, submitter | Receipt review row | Deliberate | Cannot see original receipt photo (out of scope) | "Genehmigen" / "Ablehnen" inline buttons |
| 4a. Approve | Taps "Genehmigen" | HTMX posts approval; receipt moves to approved list | Receipt row inline | Satisfied | Accidental tap risk | Single click but reversible — allow organizer to reject after approve |
| 4b. Reject | Taps "Ablehnen" | Inline rejection reason input appears | Rejection reason field | Deliberate | Reason field required — cannot dismiss without input | Predefined reason shortcuts (too vague, duplicate, wrong category) |
| 5. Submit rejection | Enters reason, confirms | HTMX posts rejection; receipt moves to rejected section with reason | Receipt row | Done | — | Participant notified on next page load |

### Key Constraint
The reviewer must not be the submitter. The system enforces this server-side. The "Genehmigen" / "Ablehnen" buttons are hidden for receipts where `submittedBy == currentMemberId`. The receipt row instead shows "Eigener Beleg" to explain the absence of review buttons.

---

## Feature 2: Settlement Summary

### Journey: Organizer Calculates Settlement

**Persona**: Organizer (role: organizer)
**Goal**: Produce a clear, actionable list of who owes whom
**Trigger**: All receipts approved; trip is COMPLETED

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Check status | Opens expense detail | Sees all receipts APPROVED; settle button enabled | Expense detail | Ready | Needs to verify all receipts are reviewed before settling | "Noch X Belege offen" warning if any SUBMITTED remain |
| 2. Initiate settle | Taps "Abrechnung abschliessen" | Confirmation dialog appears | Settle confirmation dialog | Cautious | Irreversible action — no warning currently | Dialog shows what will be calculated |
| 3. Confirm | Confirms dialog | Server calculates balances, status → SETTLED | Full page reload to settled state | Satisfied | Calculation is opaque — just sees net numbers | Show transfer list immediately |
| 4. Read summary | Reviews settlement result | Total, per-category breakdown, per-participant balance, transfer list | Settlement summary section | Engaged | Raw balance numbers are confusing ("Alice: +45.00 EUR") | Plain language transfers: "Alice zahlt Bob 45,00 EUR" |
| 5. Share | Wants to share result | No share function currently | — | Uncertain | Cannot easily share with participants | Future: copy-to-clipboard / PDF export |
| 6. Participants check | Participants open expense page | Same settled view, same transfer list | Expense detail (read-only) | Informed | Participants can view but not edit settled expense | Clear "read-only" indicator |

---

## Feature 3: Expense Categories

### Journey: Participant Selects Category When Submitting

**Persona**: Participant (role: participant)
**Goal**: Categorize a receipt correctly
**Trigger**: Adding a new receipt in the expense dialog

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Open dialog | Taps "Beleg hinzufügen" | Dialog opens with category dropdown | Add receipt dialog | Neutral | — | Pre-select most common category |
| 2. Select category | Taps category dropdown | 6 options: Unterkunft, Lebensmittel, Restaurant, Aktivität, Transport, Sonstiges | Category `<select>` | Quick | Hard to choose on mobile for long list | Native `<select>` is fine for 6 items |
| 3. Continue | Fills other fields | Normal form flow | Dialog | Focused | — | Category shown in receipt row after submission |

### Journey: Organizer Views Category Breakdown

**Persona**: Organizer (role: organizer)
**Goal**: Understand spending by category
**Trigger**: Opening a SETTLED expense

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Open settled expense | Navigates to /expense/{tripId} | Settled view loads | Expense detail | Curious | Category totals not visible currently | Category breakdown section in summary |
| 2. Scan breakdown | Reads category totals | Table: Kategorie | Betrag | % vom Gesamt | Category breakdown article | Focused | Numbers need context (are 30% on restaurant normal?) | — |
| 3. Drill into receipts | Wants to see which receipts | Scrolls to receipt list (filterable by category) | Receipt list | Detailed | No filter currently | Category filter above receipt list |

---

## Feature 4: Accommodation Cost Entry

### Journey: Organizer Enters Accommodation Costs

**Persona**: Organizer (role: organizer)
**Goal**: Enter a shared accommodation cost and split it fairly by nights stayed
**Trigger**: Trip has ended; accommodation cost known; stay periods set per participant

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Open add dialog | Taps "Beleg hinzufügen" | Dialog opens | Add receipt dialog | Ready | — | — |
| 2. Select Unterkunft | Chooses "Unterkunft" category | Dialog expands to show "Unterkunft — Aufteilung nach Übernachtungen" section | Accommodation sub-form | Interested | Extra fields appear unexpectedly | Animate expansion for clarity |
| 3. Enter total | Types total accommodation cost | Readonly preview: per-participant shares calculated from stay periods | Accommodation cost preview | Impressed if auto-calculation works | Stay periods must be set first or system shows error | "Aufenthaltszeiträume fehlen für: Anna" warning |
| 4. Review shares | Reads per-participant breakdown | Table: Name | Nächte | Anteil | Betrag | Accommodation preview table | Satisfied | System does the maths | — |
| 5. Confirm | Submits form | Receipt created with type ACCOMMODATION, per-participant shares calculated | Receipt list | Done | Shares are not adjustable after creation | Allow override in future |

**Prerequisite dependency**: Stay periods must be set for all participants in the Trips SCS. The expense SCS reads stay-period data from the TripProjection (projected from Trips events). If stay periods are missing, the accommodation form shows a warning and blocks submission.
