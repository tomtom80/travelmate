# Component Specifications: Shopping List + Email Notification — Iteration 8

**Version**: Iteration 8 design (v0.9.0-SNAPSHOT)
**Status**: Design specification — no code written
**Companion documents**:
- Wireframes: `docs/design/wireframes/shopping-list-iteration8.md`
- User journeys: `docs/design/journeys/shopping-list-iteration8-flows.md`

---

## Architecture Decision: Dedicated Page vs. Inline Section

The Shopping List is a **dedicated page** at `/trips/{id}/shoppinglist`, consistent with the Meal Plan pattern (`/trips/{id}/mealplan`). It is not inlined into the trip detail page.

**Rationale:**
- Trip detail already has 5 `<article>` sections (metadata, participants, invitations, meal plan, expense). Adding a full shopping list inline would make the detail page excessively long.
- Participants use the shopping list on mobile at the grocery store — a dedicated page provides focused, full-screen list management without surrounding trip management UI.
- The dedicated page allows HTMX polling to be scoped to a single container without interfering with other trip detail interactions.

**Integration point on Trip detail:** A new `<article>` card, visible whenever a meal plan exists for the trip:

```html
<article th:if="${hasMealPlan}">
    <h2 th:text="#{shoppinglist.title}">Einkaufsliste</h2>
    <p th:text="#{shoppinglist.hint}">Zutaten aus dem Essensplan und eigene Einträge.</p>
    <a th:href="@{/{id}/shoppinglist(id=${trip.tripId()})}" role="button"
       th:text="#{shoppinglist.view}">Einkaufsliste anzeigen</a>
</article>
```

---

## 1. ShoppingListPage (main container)

**Page**: `/trips/{id}/shoppinglist`
**Template**: `trip/shoppinglist.html`
**PicoCSS Base**: `<hgroup>` + `<article>` sections

### Structure

The page has two `<article>` sections:

1. **"Aus dem Essensplan"** — aggregated ingredients generated from assigned recipes in the meal plan, scaled to participant count.
2. **"Manuelle Einträge"** — custom items added by any participant.

Both sections share the same item row component and status lifecycle.

### Page-level variables (passed from controller)

| Variable | Type | Description |
|----------|------|-------------|
| `trip` | `TripRepresentation` | Trip name, id, dates |
| `recipeItems` | `List<ShoppingItemRepresentation>` | Auto-generated from meal plan |
| `manualItems` | `List<ShoppingItemRepresentation>` | Manually added items |
| `currentMemberId` | `String` | For self/other distinction in actions |
| `participantCount` | `int` | For scaling info display |
| `filter` | `String` | Active filter: `all`, `open`, `assigned`, `purchased` (default: `all`) |

### Recipe item scaling info

The server calculates scaled quantities server-side: `ingredient.quantity × (participantCount / recipe.servings)`. The representation includes:
- `scaledQuantity` (rounded, formatted string)
- `unit` (from ingredient)
- `sourceRecipeName` (name of originating recipe)
- `scalingNote` (e.g., "×2" or null if 1:1)

### Filter links

```html
<nav aria-label="#{shoppinglist.filter}">
    <ul>
        <li><a th:href="@{/{id}/shoppinglist(id=${trip.tripId()})}"
               th:classappend="${filter == 'all'} ? 'active' : ''"
               th:aria-current="${filter == 'all'} ? 'page' : null"
               th:text="#{shoppinglist.filter.all}">Alle</a></li>
        <li><a th:href="@{/{id}/shoppinglist(id=${trip.tripId()},filter='open')}"
               th:classappend="${filter == 'open'} ? 'active' : ''"
               th:aria-current="${filter == 'open'} ? 'page' : null"
               th:text="#{shoppinglist.filter.open}">Ausstehend</a></li>
        <li><a th:href="@{/{id}/shoppinglist(id=${trip.tripId()},filter='assigned')}"
               th:aria-current="${filter == 'assigned'} ? 'page' : null"
               th:text="#{shoppinglist.filter.assigned}">Übernommen</a></li>
        <li><a th:href="@{/{id}/shoppinglist(id=${trip.tripId()},filter='purchased')}"
               th:aria-current="${filter == 'purchased'} ? 'page' : null"
               th:text="#{shoppinglist.filter.purchased}">Erledigt</a></li>
    </ul>
</nav>
```

Filter links are plain `<a>` links (full page reload). The filter is applied server-side before rendering the item lists. This requires no client-side state and works without JavaScript.

---

## 2. ShoppingItemRow

**Template fragment**: `trip/shoppinglist :: itemRow`
**PicoCSS Base**: `<tr>` in desktop table, `<article>` in mobile card layout
**HTMX target**: individual row replacement (`hx-swap="outerHTML"`)

### ShoppingItemRepresentation fields

| Field | Type | Notes |
|-------|------|-------|
| `itemId` | `UUID` | Used in DOM id: `item-row-{itemId}` |
| `name` | `String` | Ingredient or custom item name |
| `quantity` | `String` | Pre-formatted (e.g., "400", "1.5") |
| `unit` | `String` | g, kg, l, ml, Stk., etc. |
| `status` | `ShoppingItemStatus` | `OPEN`, `ASSIGNED`, `PURCHASED` |
| `assigneeName` | `String` or null | First name of assignee |
| `assignedToCurrentUser` | `boolean` | True when current user is the assignee |
| `sourceRecipeName` | `String` or null | Null for manual items |
| `scalingNote` | `String` or null | e.g., "×2" |
| `isManual` | `boolean` | True for manually added items |

### Status badge rendering

| Status code | Label (DE) | Label (EN) | CSS class |
|-------------|-----------|------------|-----------|
| `OPEN` | Ausstehend | Open | `status-open` |
| `ASSIGNED` | Übernommen | Assigned | `status-assigned` |
| `PURCHASED` | Erledigt | Done | `status-purchased` |

```css
/* In static/css/style.css — trips module */
.status-open       { background: var(--pico-muted-background); color: var(--pico-muted-color); }
.status-assigned   { background: var(--pico-primary-background); color: var(--pico-primary); }
.status-purchased  { background: #dcfce7; color: #166534; }

/* Purchased item name styling */
.item-purchased { text-decoration: line-through; opacity: 0.6; }

/* Update highlight animation (for HTMX polling updates) */
@keyframes highlightUpdate {
    from { background-color: var(--pico-primary-background); }
    to   { background-color: transparent; }
}
.item-updated { animation: highlightUpdate 1.5s ease-out forwards; }
```

### Desktop table row template (fragment)

```html
<tr th:fragment="itemRow(item, currentMemberId)"
    th:id="'item-row-' + ${item.itemId()}"
    th:classappend="${item.status() == 'PURCHASED'} ? 'item-purchased-row' : ''">

    <!-- Name + recipe source -->
    <td>
        <span th:classappend="${item.status() == 'PURCHASED'} ? 'item-purchased' : ''"
              th:text="${item.name()}"></span>
        <br th:if="${item.sourceRecipeName() != null}">
        <small th:if="${item.sourceRecipeName() != null}" class="item-source"
               th:text="${item.sourceRecipeName() + (item.scalingNote() != null ? ' (' + item.scalingNote() + ')' : '')}">
        </small>
    </td>

    <!-- Quantity + unit -->
    <td th:text="${item.quantity()}"></td>
    <td th:text="${item.unit()}"></td>

    <!-- Assignee -->
    <td>
        <span th:if="${item.assignedToCurrentUser()}" th:text="#{shoppinglist.item.you}">Du</span>
        <span th:if="${!item.assignedToCurrentUser() and item.assigneeName() != null}"
              th:text="${item.assigneeName()}"></span>
        <span th:if="${item.assigneeName() == null}">—</span>
    </td>

    <!-- Status badge -->
    <td>
        <mark th:class="'status-' + ${item.status().toLowerCase()}"
              th:text="#{__{'shoppinglist.item.status.' + item.status()}__}">
        </mark>
    </td>

    <!-- Actions -->
    <td>
        <!-- OPEN: assign to self -->
        <form th:if="${item.status() == 'OPEN'}"
              hx-post th:hx-post="@{/{tid}/shoppinglist/items/{iid}/assign(tid=${tripId},iid=${item.itemId()})}"
              th:id="'assign-form-' + ${item.itemId()}"
              hx-target th:hx-target="'#item-row-' + ${item.itemId()}"
              hx-swap="outerHTML"
              hx-disabled-elt="find [type=submit]"
              style="margin-bottom:0;">
            <button type="submit" class="outline"
                    style="padding:0.25rem 0.75rem; white-space:nowrap;"
                    th:text="#{shoppinglist.item.assign}">Ich übernehme</button>
        </form>

        <!-- ASSIGNED by current user: unassign + mark done -->
        <div th:if="${item.status() == 'ASSIGNED' and item.assignedToCurrentUser()}"
             style="display:flex; gap:0.5rem; flex-wrap:wrap;">
            <form hx-post th:hx-post="@{/{tid}/shoppinglist/items/{iid}/unassign(tid=${tripId},iid=${item.itemId()})}"
                  hx-target th:hx-target="'#item-row-' + ${item.itemId()}"
                  hx-swap="outerHTML"
                  hx-disabled-elt="find [type=submit]"
                  style="margin-bottom:0;">
                <button type="submit" class="outline secondary"
                        style="padding:0.25rem 0.75rem;"
                        th:text="#{shoppinglist.item.unassign}">Abgeben</button>
            </form>
            <form hx-post th:hx-post="@{/{tid}/shoppinglist/items/{iid}/purchase(tid=${tripId},iid=${item.itemId()})}"
                  hx-target th:hx-target="'#item-row-' + ${item.itemId()}"
                  hx-swap="outerHTML"
                  hx-disabled-elt="find [type=submit]"
                  style="margin-bottom:0;">
                <button type="submit" class="outline"
                        style="padding:0.25rem 0.75rem;"
                        th:text="#{shoppinglist.item.purchase}">Erledigt</button>
            </form>
        </div>

        <!-- ASSIGNED by another user: no actions -->
        <!-- PURCHASED: no actions -->
    </td>

    <!-- Remove (manual items only, OPEN status only) -->
    <td th:if="${item.isManual() and item.status() == 'OPEN'}">
        <form hx-delete th:hx-delete="@{/{tid}/shoppinglist/items/{iid}(tid=${tripId},iid=${item.itemId()})}"
              hx-target th:hx-target="'#item-row-' + ${item.itemId()}"
              hx-swap="outerHTML swap:0.3s"
              hx-confirm th:hx-confirm="#{shoppinglist.item.delete.confirm}"
              style="margin-bottom:0;">
            <button type="submit" class="outline secondary"
                    style="padding:0.25rem 0.75rem;"
                    th:text="#{common.remove}">Entfernen</button>
        </form>
    </td>
    <td th:unless="${item.isManual() and item.status() == 'OPEN'}"></td>
</tr>
```

**HTMX pattern**: Each action targets its own row via `hx-target="#item-row-{id}"` and `hx-swap="outerHTML"`. The server returns the updated `<tr>` fragment. This is the same pattern as the stay-period form in the trip detail participants table.

**Remove animation**: `hx-swap="outerHTML swap:0.3s"` — HTMX fades the row out over 300ms before replacing with empty string. Requires `.htmx-swapping { opacity: 0; transition: opacity 0.3s; }` in style.css.

---

## 3. AddManualItemForm

**PicoCSS Base**: Inline `<form>` on desktop, `<dialog>` on mobile
**HTMX target**: `#manual-items-tbody` (tbody of manual items table), `innerHTML`

### Desktop inline form

```html
<tfoot id="add-item-form-row">
    <tr>
        <td colspan="7">
            <form hx-post th:hx-post="@{/{id}/shoppinglist/items(id=${trip.tripId()})}"
                  hx-target="#manual-items-tbody"
                  hx-swap="innerHTML"
                  hx-disabled-elt="find [type=submit]"
                  hx-on::after-request="if(event.detail.successful){ this.reset(); }"
                  style="margin-bottom:0;">
                <div class="grid" style="grid-template-columns: 3fr 1fr 1fr auto; gap:0.5rem; align-items:end;">
                    <label style="margin-bottom:0;">
                        <span class="sr-only" th:text="#{shoppinglist.item.name}">Eintrag</span>
                        <input type="text" name="name" required
                               th:placeholder="#{shoppinglist.item.name.placeholder}"
                               style="margin-bottom:0;">
                    </label>
                    <label style="margin-bottom:0;">
                        <span class="sr-only" th:text="#{shoppinglist.item.quantity}">Menge</span>
                        <input type="text" name="quantity" inputmode="decimal"
                               th:placeholder="#{shoppinglist.item.quantity.placeholder}"
                               style="margin-bottom:0;">
                    </label>
                    <label style="margin-bottom:0;">
                        <span class="sr-only" th:text="#{shoppinglist.item.unit}">Einheit</span>
                        <input type="text" name="unit"
                               th:placeholder="#{shoppinglist.item.unit.placeholder}"
                               style="margin-bottom:0;">
                    </label>
                    <button type="submit"
                            th:aria-label="#{shoppinglist.item.add}"
                            style="margin-bottom:0; white-space:nowrap;">+</button>
                </div>
            </form>
        </td>
    </tr>
</tfoot>
```

After submit, the server returns an updated `<tbody>` (the full manual items tbody including the newly added row). The form resets via `hx-on::after-request`. The new row enters the DOM with `class="item-updated"` for the highlight animation.

### Mobile add dialog

The mobile `<dialog>` is triggered by a floating button "Eintrag hinzufügen" at the bottom of the list. It submits to the same endpoint.

```html
<!-- Mobile: trigger button -->
<button class="outline" style="width:100%; margin-top:1rem;"
        onclick="document.getElementById('add-item-dialog').showModal()"
        th:text="#{shoppinglist.item.add.button}">
    Eintrag hinzufügen
</button>

<!-- Mobile: dialog (present on page, hidden until opened) -->
<dialog id="add-item-dialog">
    <article>
        <header>
            <button aria-label="Close" rel="prev"
                    onclick="document.getElementById('add-item-dialog').close()"></button>
            <h3 th:text="#{shoppinglist.item.add}">Eintrag hinzufügen</h3>
        </header>
        <form hx-post th:hx-post="@{/{id}/shoppinglist/items(id=${trip.tripId()})}"
              hx-target="#manual-items-tbody"
              hx-swap="innerHTML"
              hx-disabled-elt="find [type=submit]"
              hx-on::after-request="if(event.detail.successful){ this.reset(); document.getElementById('add-item-dialog').close(); }">
            <label>
                <span th:text="#{shoppinglist.item.name}">Name</span>
                <input type="text" name="name" required
                       th:placeholder="#{shoppinglist.item.name.placeholder}">
            </label>
            <div class="grid">
                <label>
                    <span th:text="#{shoppinglist.item.quantity}">Menge</span>
                    <input type="text" name="quantity" inputmode="decimal"
                           th:placeholder="#{shoppinglist.item.quantity.placeholder}">
                </label>
                <label>
                    <span th:text="#{shoppinglist.item.unit}">Einheit</span>
                    <input type="text" name="unit"
                           th:placeholder="#{shoppinglist.item.unit.placeholder}">
                </label>
            </div>
            <footer>
                <div style="display:flex; gap:0.5rem; justify-content:flex-end;">
                    <button type="button" class="secondary"
                            onclick="document.getElementById('add-item-dialog').close()"
                            th:text="#{common.cancel}">Abbrechen</button>
                    <button type="submit" th:text="#{shoppinglist.item.add}">Hinzufügen</button>
                </div>
            </footer>
        </form>
    </article>
</dialog>
```

**Responsive visibility**: The inline `<tfoot>` form is hidden on mobile via CSS. The dialog trigger button is hidden on desktop:

```css
@media (max-width: 640px) {
    .add-item-inline { display: none; }
}
@media (min-width: 641px) {
    .add-item-mobile-trigger { display: none; }
}
```

---

## 4. ShoppingListPolling (HTMX polling container)

**PicoCSS Base**: `<div>` wrapper with `hx-trigger="every 5s"`
**Target**: the polling container itself (`hx-target="this"`, `hx-swap="innerHTML"`)

The polling fetches the item list fragment only — not the whole page.

```html
<div id="shopping-list-content"
     hx-get th:hx-get="@{/{id}/shoppinglist/items(id=${trip.tripId()},filter=${filter})}"
     hx-trigger="every 5s"
     hx-target="#shopping-list-content"
     hx-swap="innerHTML">

    <!-- Item tables rendered here (both recipe and manual sections) -->
    <div th:replace="~{trip/shoppinglist :: itemLists}"></div>
</div>
```

The polling endpoint `GET /trips/{id}/shoppinglist/items` returns only the `itemLists` fragment. The server sets `HX-Trigger: none` if no changes occurred since the last request (using an ETag or content hash), which prevents HTMX from applying a no-op swap. This avoids unnecessary DOM flicker on unchanged polls.

**Server-side change detection**: The controller includes a content hash of the item list in the response via `HX-Reswap: none` when unchanged. This is a progressive enhancement — if the header is absent, HTMX applies the swap normally.

**No loading indicator on polling**: The polling request does NOT show a loading indicator (no `hx-indicator`). Silent background updates are correct here — showing a spinner every 5 seconds would be distracting for a participant actively shopping.

---

## 5. "Regenerate from Meal Plan" Action

When the meal plan changes (recipe assigned, slot status changed), the recipe-sourced shopping items become stale. The section header includes an "Aktualisieren" button:

```html
<article>
    <header>
        <h2 th:text="#{shoppinglist.section.fromRecipes}">Aus dem Essensplan</h2>
        <form method="post"
              th:action="@{/{id}/shoppinglist/regenerate(id=${trip.tripId()})}"
              style="margin-left:auto; margin-bottom:0;">
            <button type="submit" class="outline"
                    style="padding:0.25rem 0.75rem;"
                    th:text="#{shoppinglist.regenerate}">Aktualisieren</button>
        </form>
    </header>
    <!-- ... items ... -->
</article>
```

This is a standard form POST (full page reload after regeneration). The server recalculates recipe items from the current meal plan state and replaces all recipe-sourced items, preserving ASSIGNED and PURCHASED statuses where the item name+unit matches.

**Design decision: full page reload vs. HTMX**: Full page reload is correct here. Regeneration potentially changes many items simultaneously. Partial HTMX updates for a bulk operation would require complex diffing logic server-side for no meaningful UX improvement.

---

## 6. InvitationEmail Component

**Template**: `email/invitation.html` in IAM SCS (`travelmate-iam`)
**Rendered by**: `InvitationEmailSender` (Spring Mail + Thymeleaf)
**Tech**: HTML email with inline CSS (no external stylesheets — email client compatibility)

### Variables available in email template

| Variable | Type | Notes |
|----------|------|-------|
| `inviteeName` | `String` | First name of invitee |
| `inviterName` | `String` | Full name of organizer/inviter |
| `tripName` | `String` | Trip name |
| `tripStartDate` | `LocalDate` | Formatted per locale |
| `tripEndDate` | `LocalDate` | Formatted per locale |
| `acceptUrl` | `String` | Absolute URL to acceptance endpoint |
| `expiryDays` | `int` | Days until invitation expires (default: 7) |

### Email color palette (inline CSS only)

| Purpose | Value |
|---------|-------|
| Background | `#f9fafb` |
| Content background | `#ffffff` |
| Primary text | `#111827` |
| Muted text | `#6b7280` |
| CTA button background | `#2563eb` (PicoCSS default blue) |
| CTA button text | `#ffffff` |
| Trip info box background | `#eff6ff` |
| Trip info box border | `#bfdbfe` |
| Divider | `#e5e7eb` |
| Footer text | `#9ca3af` |

### Email structure (Thymeleaf template sketch)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="#{email.invitation.subject}">Reise-Einladung</title>
</head>
<body style="margin:0; padding:0; background-color:#f9fafb; font-family:sans-serif;">

<table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb; padding:24px 0;">
  <tr>
    <td align="center">
      <!-- Email container — max 600px -->
      <table width="600" cellpadding="0" cellspacing="0"
             style="background:#ffffff; border-radius:8px; overflow:hidden; max-width:600px; width:100%;">

        <!-- Header / brand -->
        <tr>
          <td style="background-color:#2563eb; padding:24px 32px;">
            <span style="font-size:22px; font-weight:700; color:#ffffff; letter-spacing:-0.5px;">
              Travelmate
            </span>
          </td>
        </tr>

        <!-- Body -->
        <tr>
          <td style="padding:32px;">

            <p style="font-size:16px; color:#111827; margin:0 0 16px;">
              <span th:text="#{email.invitation.greeting(${inviteeName})}">Hallo [Name],</span>
            </p>

            <p style="font-size:16px; color:#111827; margin:0 0 24px;">
              <span th:text="#{email.invitation.body(${inviterName})}">
                [Einladender] hat dich zu einer Reise eingeladen.
              </span>
            </p>

            <!-- Trip info box -->
            <table width="100%" cellpadding="0" cellspacing="0"
                   style="background:#eff6ff; border:1px solid #bfdbfe;
                          border-radius:6px; margin-bottom:24px;">
              <tr>
                <td style="padding:16px 20px;">
                  <p style="font-size:18px; font-weight:600; color:#1e40af; margin:0 0 8px;"
                     th:text="${tripName}">Reisename</p>
                  <p style="font-size:14px; color:#374151; margin:0;"
                     th:text="${#temporals.format(tripStartDate, 'dd.MM.yyyy', #locale)
                                + ' – '
                                + #temporals.format(tripEndDate, 'dd.MM.yyyy', #locale)}">
                    12.07.2026 – 19.07.2026
                  </p>
                </td>
              </tr>
            </table>

            <!-- CTA button -->
            <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
              <tr>
                <td align="center">
                  <a th:href="${acceptUrl}"
                     style="display:inline-block; background-color:#2563eb; color:#ffffff;
                            font-size:16px; font-weight:600; padding:14px 32px;
                            border-radius:6px; text-decoration:none;">
                    <span th:text="#{email.invitation.cta}">Einladung annehmen</span>
                  </a>
                </td>
              </tr>
            </table>

            <!-- Fallback link -->
            <p style="font-size:13px; color:#6b7280; word-break:break-all; margin:0 0 16px;">
              <span th:text="#{email.invitation.fallbackLink}">
                Oder klicke auf diesen Link:
              </span><br>
              <a th:href="${acceptUrl}" th:text="${acceptUrl}"
                 style="color:#2563eb;"></a>
            </p>

            <!-- Expiry notice -->
            <p style="font-size:13px; color:#6b7280; margin:0;"
               th:text="#{email.invitation.expiry(${expiryDays})}">
              Diese Einladung läuft in 7 Tagen ab.
            </p>

          </td>
        </tr>

        <!-- Divider -->
        <tr>
          <td style="padding:0 32px;">
            <hr style="border:none; border-top:1px solid #e5e7eb; margin:0;">
          </td>
        </tr>

        <!-- Footer -->
        <tr>
          <td style="padding:20px 32px;">
            <p style="font-size:12px; color:#9ca3af; margin:0;">
              Travelmate — Gemeinsam planen.
              <span th:text="#{email.invitation.footer}">
                Diese E-Mail wurde automatisch versandt.
              </span>
            </p>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>

</body>
</html>
```

### Accessibility for email

- Subject line is the primary accessibility hook for screen reader users in their mail client — use `#{email.invitation.subject}` with trip name included: "Einladung zur Reise: {tripName}" / "Invitation to trip: {tripName}"
- CTA button is an `<a>` (not `<button>`) — required for email clients
- Fallback plain-text link always provided below the CTA
- No image-only content — all information is present as text

### Responsive behaviour (email)

At max-width 600px (inline media query or `@media` in `<head>`):

```html
<style>
  @media (max-width: 600px) {
    table[width="600"] { width: 100% !important; }
    td[style*="padding:32px"] { padding: 20px 16px !important; }
    a[style*="padding:14px 32px"] { padding: 12px 20px !important; font-size: 14px !important; }
  }
</style>
```

Most email clients respect `@media` in `<head>` for mobile rendering. The table-based layout ensures graceful degradation where media queries are not supported.

---

## 7. HTMX Interaction Map

| User action | Trigger | HTMX method + path | Target | Swap |
|-------------|---------|-------------------|--------|------|
| Assign item to self | Button click | `hx-post /{tid}/shoppinglist/items/{iid}/assign` | `#item-row-{iid}` | `outerHTML` |
| Unassign item | Button click | `hx-post /{tid}/shoppinglist/items/{iid}/unassign` | `#item-row-{iid}` | `outerHTML` |
| Mark item purchased | Button click | `hx-post /{tid}/shoppinglist/items/{iid}/purchase` | `#item-row-{iid}` | `outerHTML` |
| Add manual item (desktop) | Form submit | `hx-post /{tid}/shoppinglist/items` | `#manual-items-tbody` | `innerHTML` |
| Add manual item (mobile dialog) | Form submit in dialog | `hx-post /{tid}/shoppinglist/items` | `#manual-items-tbody` | `innerHTML` |
| Remove manual item | Button click + confirm | `hx-delete /{tid}/shoppinglist/items/{iid}` | `#item-row-{iid}` | `outerHTML` |
| Regenerate from meal plan | Form submit (full page) | `POST /{tid}/shoppinglist/regenerate` | Full page | Full reload |
| Poll for updates | `every 5s` trigger | `hx-get /{tid}/shoppinglist/items` | `#shopping-list-content` | `innerHTML` |
| Filter by status | Link click (full page) | `GET /{tid}/shoppinglist?filter=...` | Full page | Full reload |

### Loading states

All write actions (assign, unassign, purchase, add, remove) use:

```html
hx-disabled-elt="find [type=submit]"
```

The submit button shows `aria-busy="true"` while the request is in flight. PicoCSS renders a spinner on `aria-busy="true"` buttons automatically.

---

## 8. i18n Key Reference

### New keys required — Trips SCS

#### Shopping list page and navigation

| Key | DE | EN |
|-----|----|----|
| `shoppinglist.title` | Einkaufsliste | Shopping List |
| `shoppinglist.hint` | Zutaten aus dem Essensplan und eigene Einträge. | Ingredients from the meal plan and custom items. |
| `shoppinglist.view` | Einkaufsliste anzeigen | View Shopping List |
| `shoppinglist.filter` | Liste filtern | Filter list |
| `shoppinglist.filter.all` | Alle | All |
| `shoppinglist.filter.open` | Ausstehend | Open |
| `shoppinglist.filter.assigned` | Übernommen | Assigned |
| `shoppinglist.filter.purchased` | Erledigt | Done |
| `shoppinglist.section.fromRecipes` | Aus dem Essensplan | From Meal Plan |
| `shoppinglist.section.manual` | Manuelle Einträge | Custom Items |
| `shoppinglist.empty` | Keine Einträge vorhanden. | No items yet. |
| `shoppinglist.empty.filtered` | Keine Einträge für diesen Filter. | No items matching this filter. |
| `shoppinglist.regenerate` | Aktualisieren | Refresh |
| `shoppinglist.scalingInfo` | Mengen skaliert auf {0} Personen. | Quantities scaled for {0} people. |

#### Item status labels

| Key | DE | EN |
|-----|----|----|
| `shoppinglist.item.status.OPEN` | Ausstehend | Open |
| `shoppinglist.item.status.ASSIGNED` | Übernommen | Assigned |
| `shoppinglist.item.status.PURCHASED` | Erledigt | Done |

#### Item actions

| Key | DE | EN |
|-----|----|----|
| `shoppinglist.item.assign` | Ich übernehme | I'll get it |
| `shoppinglist.item.unassign` | Abgeben | Unassign |
| `shoppinglist.item.purchase` | Erledigt | Done |
| `shoppinglist.item.you` | Du | You |
| `shoppinglist.item.add` | Eintrag hinzufügen | Add Item |
| `shoppinglist.item.add.button` | + Eintrag hinzufügen | + Add Item |
| `shoppinglist.item.delete.confirm` | Eintrag wirklich entfernen? | Remove this item? |

#### Item form fields

| Key | DE | EN |
|-----|----|----|
| `shoppinglist.item.name` | Name | Name |
| `shoppinglist.item.name.placeholder` | z.B. Haferflocken | e.g. Oats |
| `shoppinglist.item.quantity` | Menge | Quantity |
| `shoppinglist.item.quantity.placeholder` | z.B. 400 | e.g. 400 |
| `shoppinglist.item.unit` | Einheit | Unit |
| `shoppinglist.item.unit.placeholder` | z.B. g | e.g. g |

### New keys required — IAM SCS (email)

| Key | DE | EN |
|-----|----|----|
| `email.invitation.subject` | Einladung zur Reise: {0} | Invitation to trip: {0} |
| `email.invitation.greeting` | Hallo {0}, | Hello {0}, |
| `email.invitation.body` | {0} hat dich zu einer Reise eingeladen. | {0} has invited you to a trip. |
| `email.invitation.cta` | Einladung annehmen | Accept Invitation |
| `email.invitation.fallbackLink` | Oder klicke auf diesen Link: | Or click this link: |
| `email.invitation.expiry` | Diese Einladung läuft in {0} Tagen ab. | This invitation expires in {0} days. |
| `email.invitation.footer` | Diese E-Mail wurde automatisch versandt. | This email was sent automatically. |

---

## 9. Domain Model Notes (for backend alignment)

These are design-level observations, not prescriptive architecture decisions.

### ShoppingItem states

```
OPEN ──[assign]──► ASSIGNED ──[purchase]──► PURCHASED
  ▲                    │                       │
  └────[unassign]──────┘                       │
                       ▲                       │
                       └───[undoPurchase]───────┘

OPEN ──[directPurchase]──► PURCHASED  (implicit assign + purchase in one step)
```

- Only the assigned participant can unassign or mark as purchased.
- Any participant can assign an OPEN item.
- PURCHASED items can be reverted to ASSIGNED (undo accidental mobile taps).
- OPEN items can be directly purchased (implicit assignment + purchase in one step).
- Manual items can be removed (deleted) when OPEN. Recipe items cannot be removed (they are regenerated from the meal plan).

### Quantity scaling

The shopping list controller (or application service) calculates scaled quantities:

```
scaledQuantity = ingredient.quantity × ceil(participantCount / recipe.servings)
```

Quantities are aggregated across meal plan slots using the same ingredient. Example: if "Milch" appears in 3 recipes each requiring 0.5l, and each recipe is used once for 4 people scaled from 2 servings, the aggregated quantity is 3 × (0.5 × ceil(4/2)) = 3 × 1.0 = 3.0 l.

Aggregation key: `(normalised ingredient name, unit)`. Normalisation is simple (lowercase, trim). No NLP ingredient matching — keep it simple.

### Stale item handling on regeneration

When "Aktualisieren" is triggered after meal plan changes:
1. All OPEN recipe items are deleted and regenerated.
2. ASSIGNED and PURCHASED recipe items with matching `(name, unit)` retain their status and assignee.
3. ASSIGNED and PURCHASED items whose source recipe is no longer in the meal plan are moved to manual items (preserving status).

This prevents data loss when the meal plan is updated while shopping is already in progress.
