# Plan: SVG-Deduplizierung via Thymeleaf-Fragments

## Context

48 Inline-SVGs verteilt auf 27 Templates in 3 Modulen. Viele Icons (Edit, Delete, Trash, Menu) sind 5-7x identisch kopiert. Hero-Illustrationen (5-18 Zeilen) blähen Templates auf. Ziel: Deduplizierung und bessere Lesbarkeit, ohne CSS-Integration oder HTMX-Kompatibilität zu verlieren.

**Warum Fragments statt externer Dateien:** Die Hero-SVGs werden über `.page-hero__art svg { stroke: var(--tm-primary); }` per CSS gestylt. Bei `<img src="hero.svg">` cascaded CSS nicht in die SVG-Interna — das Styling ginge verloren. Thymeleaf-Fragments bleiben inline im DOM und erhalten das CSS-Styling vollständig.

---

## Schritt 1: Icon-Fragments erstellen (pro Modul)

Neue Datei `fragments/icons.html` in jedem Modul mit allen genutzten Icons als benannte Fragments.

### travelmate-iam: `src/main/resources/templates/fragments/icons.html`
- `icon-menu` — Hamburger-Menü (20x20)
- `icon-edit` — Pencil/Bearbeiten (24x24)
- `icon-delete` — X/Löschen (24x24)

### travelmate-trips: `src/main/resources/templates/fragments/icons.html`
- `icon-edit` — Pencil/Bearbeiten
- `icon-delete` — X/Löschen
- `icon-trash` — Mülleimer
- `icon-check` — Häkchen/Bestätigen
- `icon-decline` — X/Ablehnen
- `icon-dots` — Drei-Punkte-Menü
- `icon-menu` — Hamburger-Menü

### travelmate-expense: `src/main/resources/templates/fragments/icons.html`
- `icon-menu` — Hamburger-Menü
- `icon-check` — Häkchen
- `icon-trash` — Mülleimer

**Fragment-Konvention:**
```html
<svg th:fragment="icon-edit" class="icon" viewBox="0 0 24 24">
    <path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
    <path d="m15 5 4 4"/>
</svg>
```

**Nutzung:**
```html
<svg th:replace="~{fragments/icons :: icon-edit}"></svg>
```

---

## Schritt 2: Hero-Fragments erstellen (pro Modul)

Neue Datei `fragments/heroes.html` in jedem Modul mit allen Hero-Illustrationen.

### travelmate-iam: `src/main/resources/templates/fragments/heroes.html`
- `hero-party` — Personen-Gruppe (Dashboard)
- `hero-feature-team` — Team-Icon (Landing)
- `hero-feature-compass` — Kompass-Icon (Landing)
- `hero-feature-card` — Karten-Icon (Landing)

### travelmate-trips: `src/main/resources/templates/fragments/heroes.html`
- `hero-planning` — Kalender/Checkliste (Trip-Index)
- `hero-suitcase` — Koffer (Trip-List, Trip-Detail, Trip-Form) — **3x wiederverwendet**
- `hero-calendar` — Kalender (DatePoll-Seiten)
- `hero-house` — Haus (AccommodationPoll + Accommodation)
- `hero-recipe` — Topf/Teller (Recipe-List)
- `hero-mealplan` — Kalender-Grid (MealPlan)
- `hero-shopping` — Einkaufskorb (ShoppingList)
- `hero-planning-step-calendar` — Schritt-Kalender (Planning)
- `hero-planning-step-checklist` — Schritt-Checkliste (Planning)

### travelmate-expense: `src/main/resources/templates/fragments/heroes.html`
- `hero-expense` — Quittung+Euro (Expense-Index)
- `hero-wallet` — Offene Geldbörse (Expense-Detail)

**Fragment-Konvention:**
```html
<svg th:fragment="hero-suitcase" viewBox="0 0 240 160">
    <rect x="60" y="50" .../>
    <!-- ... -->
</svg>
```

**Nutzung (ersetzt nur das SVG, Wrapper bleibt):**
```html
<div class="page-hero__art" aria-hidden="true">
    <svg th:replace="~{fragments/heroes :: hero-suitcase}"></svg>
</div>
```

---

## Schritt 3: Templates aktualisieren

Alle 48 Inline-SVGs durch `th:replace`-Referenzen ersetzen. Reihenfolge nach Modul:

### 3a. travelmate-iam (13 SVGs in 7 Dateien)
- `layout/default.html` — Hamburger → `icons :: icon-menu`
- `layout/public.html` — Hamburger → `icons :: icon-menu`
- `landing.html` — Hamburger + 3 Feature-Icons → `icons :: icon-menu` + `heroes :: hero-feature-*`
- `dashboard/index.html` — Edit + Hero → `icons :: icon-edit` + `heroes :: hero-party`
- `dashboard/companions.html` — Delete → `icons :: icon-delete`
- `dashboard/members.html` — Delete → `icons :: icon-delete`

### 3b. travelmate-trips (30 SVGs in 16 Dateien)
- `layout/default.html` — Hamburger
- `index.html` — Hero-Planning
- `trip/list.html` — Hero-Suitcase
- `trip/detail.html` — Hero-Suitcase + Edit + Dots
- `trip/form.html` — Hero-Suitcase
- `trip/invitations.html` — Check + Decline
- `recipe/list.html` — Hero-Recipe + Edit + Delete
- `recipe/trip-list.html` — Edit + Delete
- `mealplan/overview.html` — Hero-Mealplan
- `shoppinglist/overview.html` — Hero-Shopping
- `accommodation/overview.html` — Hero-House + Edit + Trash + Delete (x3)
- `datepoll/create.html` — Hero-Calendar + Trash (x2)
- `datepoll/overview.html` — Hero-Calendar + Trash
- `accommodationpoll/create.html` — Hero-House
- `accommodationpoll/overview.html` — Hero-House
- `planning/overview.html` — Hero-Planning + 2 Step-Cards

### 3c. travelmate-expense (5 SVGs in 4 Dateien)
- `layout/default.html` — Hamburger
- `index.html` — Hero-Expense
- `expense/detail.html` — Hero-Wallet + Check + Trash
- `expense/weightings.html` — Check

---

## Schritt 4: Layout-Hamburger konsolidieren

Das Hamburger-Icon erscheint 5x in Layout-Dateien. Nach Schritt 3 referenziert jedes Layout `fragments/icons :: icon-menu`. Falls das Hamburger-Icon in einer zentralen Stelle im Layout-Fragment schon enthalten ist, entfällt die Duplizierung automatisch.

---

## Dateien die erstellt werden (6 neue)

| Datei | Modul |
|-------|-------|
| `templates/fragments/icons.html` | travelmate-iam |
| `templates/fragments/heroes.html` | travelmate-iam |
| `templates/fragments/icons.html` | travelmate-trips |
| `templates/fragments/heroes.html` | travelmate-trips |
| `templates/fragments/icons.html` | travelmate-expense |
| `templates/fragments/heroes.html` | travelmate-expense |

## Dateien die modifiziert werden (27 Templates)

Alle 27 Template-Dateien aus Schritt 3a-3c.

---

## Verifikation

1. `./mvnw -pl travelmate-iam clean test` — IAM Tests grün
2. `./mvnw -pl travelmate-trips clean test` — Trips Tests grün
3. `./mvnw -pl travelmate-expense clean test` — Expense Tests grün
4. Manuell: Docker Compose starten, jede Seite aufrufen, prüfen dass SVGs korrekt gerendert werden und CSS-Styling (`var(--tm-primary)`) greift
5. `./mvnw -Pe2e verify` — E2E Tests grün
