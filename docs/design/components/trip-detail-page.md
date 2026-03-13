## Component: Trip Detail Page
**Page**: `/trips/{id}` — `trip/detail.html`
**PicoCSS Base**: `<article>` cards for sections, `<hgroup>` for page header

---

### Current State Assessment

The current page has three flat `<section>` blocks (trip metadata, participants, invitations) with `<h2>` headings that are visually indistinct from each other. There is no visual separation, no card treatment, and no hierarchy. The user complaint ("the invite section … isn't distinguishable from the rest") is a direct consequence of this flat structure.

The architect's proposal to add `<article>` wrappers is correct as a first step, but **the information architecture itself needs to change** before adding visual wrappers fixes anything. Specifically:

1. The page currently conflates **trip status management** (organiser actions) with **participant view**. These are different contexts.
2. **Companions are entirely absent from the participants section.** The user's goal of setting stay periods for family members has no UI path at all.
3. The **invite-by-email form** sits inline below the invite-by-dropdown form with only an `<h3>` separator. Both forms share the "Einladungen" section, which causes the confusion the user describes.

---

### Recommended Page Structure

```
┌─────────────────────────────────────────────────┐
│  hgroup: Trip name + description                │
└─────────────────────────────────────────────────┘

┌─── article: Reisedetails ───────────────────────┐
│  Dates, Status (localised label + badge)        │
│  [Organiser only] Status action buttons         │
└─────────────────────────────────────────────────┘

┌─── article: Reisegruppe ────────────────────────┐
│  h2: Reisegruppe                                │
│  Table: Name | Anreise | Abreise | Aktionen     │
│    Each member: read-only name, editable dates  │
│      — current user: inline date pickers        │
│      — companions of current user: same         │
│      — other members: read-only                 │
│  Empty state: "Noch keine Teilnehmer."          │
└─────────────────────────────────────────────────┘

┌─── article: Einladungen ────────────────────────┐
│  h2: Einladungen                                │
│  Pending invitations list (table or cards)      │
│  ─────────────────────────────────────────────  │
│  [Organiser only] "Mitglied einladen" subsection│
│    Dropdown + Send button                       │
│  [Organiser only] "Extern einladen" — button    │
│    → opens <dialog> with email form             │
└─────────────────────────────────────────────────┘
```

---

### Section 1: Trip Metadata Card

```html
<article>
  <header>
    <span th:text="#{trip.status.label}">Status</span>
    <!-- Localised status badge -->
    <mark class="status-badge" th:classappend="${'status-' + trip.status().toLowerCase()}"
          th:text="#{__{'trip.status.' + trip.status()}__}">In Planung</mark>
  </header>
  <p>
    <strong th:text="#{trip.startDate}">Von</strong>:
    <span th:text="${trip.startDate()}"></span>
    —
    <strong th:text="#{trip.endDate}">Bis</strong>:
    <span th:text="${trip.endDate()}"></span>
  </p>
  <!-- Organiser status actions — only when relevant -->
  <footer th:if="${isOrganizer and trip.status() != 'COMPLETED' and trip.status() != 'CANCELLED'}">
    <div class="grid">
      <form th:if="${trip.status() == 'PLANNING'}" method="post"
            th:action="@{/{id}/confirm(id=${trip.tripId()})}">
        <button type="submit" th:text="#{trip.confirm}">Bestätigen</button>
      </form>
      <!-- ... other status transitions ... -->
      <form method="post" th:action="@{/{id}/cancel(id=${trip.tripId()})}">
        <button type="submit" class="outline secondary"
                hx-confirm th:hx-confirm="#{trip.cancelConfirm}"
                th:text="#{trip.cancel}">Absagen</button>
      </form>
    </div>
  </footer>
</article>
```

Status badge CSS:
```css
.status-badge { font-size: 0.85rem; padding: 0.2rem 0.5rem; }
.status-planning  { background: var(--pico-primary-background); }
.status-confirmed { background: #dcfce7; color: #166534; }
.status-in_progress { background: #fef9c3; color: #854d0e; }
.status-completed { background: #f1f5f9; color: #475569; }
.status-cancelled { background: var(--tm-danger-bg); color: var(--tm-danger); }
```

---

### Section 2: Reisegruppe (Participants including Companions)

This is the most significant gap the architect's proposals do not address.

**The problem:** The current template shows an inline stay-period form only when `p.participantId() == currentMemberId`. Companions (Dependents) are not participants in the current model — they need to be added to a trip's participant list when the organiser or their owning member joins.

**The UX requirement:** The user wants to set arrival/departure dates for themselves AND their companions (dependents). This means:

- The backend must support adding companions as participants (either auto-added when the account owner joins, or explicitly managed).
- The frontend must show a row per companion, with the same editable date pickers as the member row.

**Recommended component for each editable participant row:**

```html
<tr id="participant-row-[[${p.participantId()}]]">
  <td th:text="${p.firstName() + ' ' + p.lastName()}"></td>
  <td>
    <!-- Editable dates — for current user AND their companions -->
    <form th:if="${p.isEditableByCurrentUser}"
          hx-post th:hx-post="@{/{tid}/participants/{pid}/stay-period(tid=${trip.tripId()},pid=${p.participantId()})}"
          hx-target="#participant-row-[[${p.participantId()}]]"
          hx-swap="outerHTML"
          style="margin-bottom: 0;">
      <input type="date" name="arrivalDate" th:value="${p.arrivalDate()}" required>
    </form>
    <span th:unless="${p.isEditableByCurrentUser}" th:text="${p.arrivalDate()}"></span>
  </td>
  <td>
    <form th:if="${p.isEditableByCurrentUser}" ...>
      <input type="date" name="departureDate" th:value="${p.departureDate()}" required>
    </form>
    <span th:unless="${p.isEditableByCurrentUser}" th:text="${p.departureDate()}"></span>
  </td>
  <td th:if="${p.isEditableByCurrentUser}">
    <!-- Single save button outside the individual date forms, or one form wrapping both date inputs -->
    <button type="submit" th:text="#{common.save}"
            style="white-space: nowrap; min-width: 5rem; margin-bottom: 0;">Speichern</button>
  </td>
</tr>
```

**Important:** The stay-period form should use `hx-target="#participant-row-..."` with `hx-swap="outerHTML"` so the entire `<tr>` is replaced. The server returns the updated `<tr>` including a visual confirmation ("Gespeichert") that disappears after 2 seconds via CSS animation (no JS needed):

```css
.save-confirmation {
  animation: fadeOut 2s forwards;
  animation-delay: 1s;
  color: #166534;
}
@keyframes fadeOut { to { opacity: 0; } }
```

This approach eliminates the button-grows bug entirely because the table cell is replaced, not reflowed.

**Note for backend:** Requires `TripRepresentation` to include a boolean `isEditableByCurrentUser` per participant, set server-side based on whether the participant is the current member or one of their dependents.

---

### Section 3: Invitations (Redesigned)

The architect's proposal to use a `<dialog>` for the external invite form is correct. However, **the dialog should open via a clear button, not appear as an inline form**.

The user complaint is that "there's a link to invite which isn't distinguishable from the rest." The `h3` heading acting as a section label for an always-visible form is the cause.

**Recommended structure:**

```html
<article>
  <header>
    <h2 th:text="#{invitation.title}">Einladungen</h2>
    <!-- Organiser invite actions — right-aligned in header -->
    <div th:if="${isOrganizer}" style="display:flex; gap:0.5rem; margin-left:auto;">
      <!-- Only shown when there are invitable members -->
      <button th:if="${!invitableMembers.isEmpty()}"
              onclick="document.getElementById('invite-member-dialog').showModal()"
              class="outline"
              th:text="#{invitation.invite}">Mitglied einladen</button>
      <button onclick="document.getElementById('invite-external-dialog').showModal()"
              class="outline"
              th:text="#{invitation.inviteByEmail}">Per E-Mail einladen</button>
    </div>
  </header>

  <!-- Invitation list — the main content -->
  <div id="invitations">
    <div th:replace="~{trip/invitations :: invitationList}"></div>
  </div>
</article>

<!-- Dialog: Invite member from travel party -->
<dialog id="invite-member-dialog">
  <article>
    <header>
      <h3 th:text="#{invitation.invite}">Mitglied einladen</h3>
      <button class="close" aria-label="Close"
              onclick="document.getElementById('invite-member-dialog').close()">&#x2715;</button>
    </header>
    <form hx-post th:hx-post="@{/{id}/invitations(id=${trip.tripId()})}"
          hx-target="#invitations"
          hx-swap="innerHTML"
          hx-on::after-request="if(event.detail.successful) { document.getElementById('invite-member-dialog').close(); this.reset(); }">
      <label for="inviteeId" th:text="#{invitation.member}">Mitglied</label>
      <select id="inviteeId" name="inviteeId" required>
        <option th:each="m : ${invitableMembers}"
                th:value="${m.memberId()}"
                th:text="${m.firstName() + ' ' + m.lastName()}"></option>
      </select>
      <footer>
        <button type="submit" th:text="#{invitation.send}">Einladung senden</button>
      </footer>
    </form>
  </article>
</dialog>

<!-- Dialog: Invite external person by email -->
<dialog id="invite-external-dialog">
  <article>
    <header>
      <h3 th:text="#{invitation.inviteByEmail}">Per E-Mail einladen</h3>
      <button class="close" aria-label="Close"
              onclick="document.getElementById('invite-external-dialog').close()">&#x2715;</button>
    </header>
    <form hx-post th:hx-post="@{/{id}/invitations/external(id=${trip.tripId()})}"
          hx-target="#invitations"
          hx-swap="innerHTML"
          hx-on::after-request="if(event.detail.successful) { document.getElementById('invite-external-dialog').close(); this.reset(); }">
      <div class="grid">
        <label>
          <span th:text="#{invitation.firstName}">Vorname</span>
          <input type="text" name="firstName" required>
        </label>
        <label>
          <span th:text="#{invitation.lastName}">Nachname</span>
          <input type="text" name="lastName" required>
        </label>
      </div>
      <label>
        <span th:text="#{invitation.email}">E-Mail</span>
        <input type="email" name="email" required>
      </label>
      <label>
        <span th:text="#{common.dateOfBirth}">Geburtsdatum</span>
        <input type="date" name="dateOfBirth" required>
      </label>
      <footer>
        <button type="submit" th:text="#{invitation.sendEmailInvite}">Per E-Mail einladen</button>
      </footer>
    </form>
  </article>
</dialog>
```

**Why dialog works here:** The PicoCSS native `<dialog>` is fully accessible — it traps focus, supports Escape to close, and is announced correctly by screen readers. The `onclick="...showModal()"` is a minimal JS dependency. Dialogs are initiated by a clearly labelled button in the card header, which solves the "link indistinguishable from text" problem.

**What happens after invitation sent:** The HTMX request targeting `#invitations` returns the updated invitation list with a `<p role="status">` success notice at the top ("Einladung an [Name] gesendet"). The dialog closes. The user sees the list update with a visible notice.

---

### HTMX Interactions Summary

| Trigger | Target | Swap | Loading indicator |
|---------|--------|------|-------------------|
| Stay-period form submit | `#participant-row-{id}` | `outerHTML` | `aria-busy="true"` on button |
| Invite member (in dialog) | `#invitations` | `innerHTML` | `aria-busy="true"` on dialog submit button |
| Invite external (in dialog) | `#invitations` | `innerHTML` | `aria-busy="true"` on dialog submit button |
| Accept/Decline invitation | `#invitations` | `innerHTML` | `aria-busy="true"` on button |
| Status action (confirm/start/complete) | Full page POST | — | Browser navigation |
| Cancel trip | Full page POST + `hx-confirm` dialog first | — | Browser navigation |

---

### Accessibility

- All dialogs use native `<dialog>` — native focus trap, Escape key support, `aria-modal="true"` implied
- Status badge uses colour AND text label — never colour alone
- `role="alert"` on error messages, `role="status"` on success messages
- Participant table uses `<th scope="col">` for column headers
- Stay-period date inputs have associated `<label>` elements (currently missing — inline inputs have no label in the current code)
