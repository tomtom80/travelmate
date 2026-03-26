# UI/UX Redesign Plan — Travelmate

## Context

Die aktuelle UI hat sich organisch über 12 Iterationen entwickelt. Jedes SCS hat eigene CSS-Dateien mit Copy-Paste-Duplikation, inkonsistente Tabellen-Aktionen (Text-Buttons, Inline-Forms, Kebab-Menus gemischt), zwei echte Bugs (Weightings-Save, Sortier-Flackern), und die Expense Party-Tabelle ist auf Mobile unbenutzbar. Ziel: Einheitliches Design-System mit konsistenten Patterns über alle 3 SCS.

---

## Phase 0: Design-System Grundlagen (CSS + Logo + Nav)

### 0A: SVG Logo erstellen
- Einfaches Inline-SVG: Kompass-Nadel + Koffer-Silhouette, `--pico-primary`-Blau
- Zwei Varianten: Volllogo (Mark + "Travelmate"), Icon-only (Mark für Mobile/Favicon)
- Dateien: `static/images/logo.svg`, `static/images/logo-mark.svg` (in jedem SCS)

### 0B: Shared CSS Design Tokens & Icon-Button-System
Zu allen 3 `style.css` hinzufügen:

```css
/* Icon-Buttons: 2.25rem Quadrat, SVG-Icons via data:image/svg+xml */
.btn-icon { /* quadratisch, zentriert, outline-style */ }
.btn-icon--edit   { /* Stift-SVG */ }
.btn-icon--delete { /* X-SVG */ }
.btn-icon--add    { /* Plus-SVG */ }
.btn-icon--kebab  { /* ⋮ Punkte-SVG, öffnet Dropdown */ }

/* Einheitliche Aktionsspalte */
.action-col { display:flex; gap:0.25rem; align-items:center; }
```

**Dateien:** `travelmate-{iam,trips,expense}/src/main/resources/static/css/style.css`

### 0C: Hamburger-Navigation (Mobile)
- `<details class="nav-toggle">` wrapping Nav-Links (nutzt bestehendes `<details>` Pattern)
- Desktop: `display:none` auf Toggle, Links inline
- Mobile: Hamburger-Icon (3-Balken SVG), Klick öffnet/schließt
- Logout als normaler Link-Style statt oversized Button

**Dateien:**
- `travelmate-{iam,trips,expense}/src/main/resources/templates/layout/default.html`
- `travelmate-iam/src/main/resources/templates/layout/public.html`
- `travelmate-iam/src/main/resources/templates/landing.html`
- Alle 3 `style.css` + `landing.css`

---

## Phase 1: Bug Fixes

### 1A: Weightings-Save → unstyled Seite
**Root Cause:** `ExpenseController.updateWeighting()` (Zeile 240) gibt Fragment `"expense/weightings :: weightingList"` zurück, aber das `<form>` in `weightings.html` macht einen normalen POST (kein HTMX). Browser zeigt nacktes Fragment ohne Layout.

**Fix:** HTMX-Attribute zum Weighting-Form hinzufügen:
```html
<form hx-post="..." hx-target="#weightings" hx-swap="innerHTML" ...>
```

**Dateien:**
- `travelmate-expense/src/main/resources/templates/expense/weightings.html` (Zeile 19)
- Test: `ExpenseControllerTest.java` — verify POST returns fragment with HX-Trigger header

### 1B: Teilnehmer-Sortierung flackert
**Root Cause:** `TripJpaEntity.participants` hat kein `@OrderBy`. Nach Stay-Period-Update: Redirect → JPA lädt Participants in beliebiger Reihenfolge → Eintrag springt.

**Fix:** `@OrderBy("firstName ASC, lastName ASC")` auf participants-Feld.

**Dateien:**
- `travelmate-trips/src/main/java/.../adapters/persistence/TripJpaEntity.java` (Zeile 53)
- Weitere Tabellen prüfen: MealPlan Slots (hat bereits @OrderBy ✓), ShoppingList Items, Accommodation Rooms

---

## Phase 2: Einheitliche Aktionsspalte

Alle Tabellen bekommen konsistente "Aktionen"-Spalte mit Icon-Buttons.

### Pattern:
| Primäre Aktion | Icon-Button |
|---|---|
| Bearbeiten | `.btn-icon--edit` (Stift) |
| Löschen | `.btn-icon--delete` (X) |
| Hinzufügen | `.btn-icon--add` (Plus) |
| Mehr Aktionen | `.btn-icon--kebab` (⋮) → Dropdown |

### Tabellen-Änderungen:

| Tabelle | Aktuell | Neu |
|---|---|---|
| IAM Members | Text "Löschen" Button | `.btn-icon--delete` |
| IAM Companions | Text "Löschen" Button | `.btn-icon--delete` |
| Trips Recipe List | "Bearbeiten"/"Löschen" Text-Buttons mit inline-style | `.btn-icon--edit` + `.btn-icon--delete` |
| Trips Invitations | Accept/Decline Text-Buttons | Accept=Primary small, Decline=Secondary small |
| Expense Receipts | Approve/Reject/Remove in flex-div | `.btn-icon--edit` (approve) + `.btn-icon--kebab` (reject/resubmit/remove) |
| Expense Weightings | Inline form pro Zeile | `.btn-icon--edit` → Dialog |
| Shopping List Items | Status-Buttons inline | Primary action button + `.btn-icon--kebab` für sekundäre |
| Accommodation Rooms | Inline + Delete | `.btn-icon--edit` + `.btn-icon--delete` |
| Advance Payments | Toggle-Button | `.btn-icon--edit` (toggle paid) |

**Dateien:** Alle Template-Dateien mit Tabellen (siehe oben), alle 3 `style.css`

**i18n:** `common.actions=Aktionen` / `common.actions=Actions` zu allen messages.properties

---

## Phase 3: Teilnehmer-Redesign (Trip Detail)

### 3A: Inline-Editing entfernen → Dialog
- Aktuelle participant-actions-grid mit Date-Inputs inline entfernen
- Neue Spalten: Name | Anreise | Abreise | Aktionen
- Aktionen: `.btn-icon--edit` (öffnet Dialog) + `.btn-icon--kebab` (Organizer-Rechte, Entfernen)
- Dialog: `<dialog>` mit Arrival-Date + Departure-Date + Save/Cancel
- Form im Dialog: POST zu bestehender `/{tripId}/participants/{pid}/stay-period`

### 3B: CSS aufräumen
- Entfernen: `.participant-actions-grid`, `.participant-actions-field`, `.participant-actions-label`, `.participant-actions-input`, `.participant-actions-toolbar`, `.participant-actions-primary` (ca. 70 Zeilen)
- Mobile-Overrides für diese Klassen auch entfernen

**Dateien:**
- `travelmate-trips/src/main/resources/templates/trip/detail.html`
- `travelmate-trips/src/main/resources/static/css/style.css`

---

## Phase 4: Expense Party-Tabelle Redesign

### 4A: Teilnehmer-Namen statt Email-Adressen
**Root Cause:** `ParticipantJoinedTrip.username()` enthält für Members die Email, für Dependents "Vorname Nachname". Expense speichert `username` als `TripParticipant.name()`.

**Fix (minimal-invasiv):** In `TripService.publishParticipantJoinedEvents()` und `addParticipantToTrip()`: Für Members `member.firstName() + " " + member.lastName()` statt `member.email()` als `username` übergeben. Expense zeigt dann automatisch Namen an.

**Risikoanalyse:** Kein Consumer nutzt `username` als Email für Lookups. Es ist rein für Display. Bestehende Daten in DB zeigen weiterhin Email bis neue Events kommen — akzeptabel für Entwicklung.

**Dateien:**
- `travelmate-trips/src/main/java/.../application/TripService.java` (publishParticipantJoinedEvents, addParticipantToTrip)
- Tests in `TripServiceTest.java` anpassen

### 4B: 8-Spalten-Tabelle → Card-Layout
Die Party-Account-Tabelle (8 Spalten + eingebetteter Kontoverlauf) ist auf Mobile unbenutzbar.

**Neues Layout:** Pro Reisepartei ein `<article>`-Card:
```
┌─────────────────────────────────────┐
│ Familie Müller          +125.50 EUR │  ← Header: Name + Balance (grün/rot)
│─────────────────────────────────────│
│ Anteil: 380.00 EUR                  │  ← Summary-Zeile
│ Belege: 505.50 EUR                  │
│ Anzahlung: 200/400 EUR (offen)      │
│─────────────────────────────────────│
│ ▸ Mitglieder (3)                    │  ← Klappbar
│ ▸ Kontoverlauf (5 Einträge)         │  ← Klappbar
└─────────────────────────────────────┘
```

**Dateien:**
- `travelmate-expense/src/main/resources/templates/expense/detail.html` (Zeile 98-161)
- `travelmate-expense/src/main/resources/static/css/style.css`

### 4C: Weightings → Dialog statt Inline
Statt Inline-Form pro Zeile: `.btn-icon--edit` öffnet Dialog mit Number-Input + Save.
Nutzt den in Phase 1A gefixt HTMX-Flow.

**Dateien:**
- `travelmate-expense/src/main/resources/templates/expense/weightings.html`

---

## Phase 5: Einheitliches Add/Create-Pattern

### Standard:
- "Neu erstellen/Hinzufügen"-Button immer **vor** der Tabelle, rechtsbündig
- Button-Style: `class="outline"` mit Plus-Icon
- Komplexe Formulare öffnen `<dialog>` (wie bestehende Invite-Dialoge)

### Änderungen:
| Stelle | Aktuell | Neu |
|---|---|---|
| Trip-Liste | "Neue Reise" Link unten | Button oben vor Tabelle |
| IAM Member-Einladung | Form unter Tabelle | Dialog + Trigger-Button oben |
| IAM Companion hinzufügen | Form unter Tabelle | Dialog + Trigger-Button oben |
| Rezept-Liste | "Neues Rezept" Link oben | Konsistenter Button-Style |
| Shopping List Manual Item | Inline-Form in Footer | Konsistenter Button → Dialog |

**Dateien:**
- `travelmate-trips/src/main/resources/templates/trip/list.html`
- `travelmate-iam/src/main/resources/templates/dashboard/index.html`
- `travelmate-trips/src/main/resources/templates/recipe/list.html`
- `travelmate-trips/src/main/resources/templates/shoppinglist/overview.html`

---

## Phase 6: Landing Page + Logo Integration

- Logo in alle Nav-Bars (Mark + Wordmark Desktop, Mark-only Mobile)
- Landing Page Hero: Volllogo statt Text-only
- Feature-Icons: SVG statt Unicode-Emoji (✈️→SVG, 🏡→SVG, 💰→SVG)
- Logout-Button: Ghost-Style statt Outlined (weniger visuelles Gewicht)

**Dateien:**
- `travelmate-iam/src/main/resources/templates/landing.html`
- `travelmate-iam/src/main/resources/static/css/landing.css`
- Alle `layout/default.html`

---

## Phase 7: Tests

### Controller-Tests:
- `ExpenseControllerTest`: Weightings POST returns fragment (not redirect)
- `TripControllerTest`: Verify dialog model attributes for participant editing
- `DashboardControllerTest`: Verify dialog model attributes

### Persistence-Tests:
- `TripRepositoryAdapter`: Participants loaded in alphabetical order after @OrderBy

### E2E (manuell):
- Volles E2E-Suite nach allen Phasen
- Speziell: Weightings Save, Participant Stay-Period via Dialog, Mobile Navigation

---

## Phase 8: Dokumentation
- `docs/design/components/design-system.md` — Icon-Buttons, Action-Columns, Dialog-Pattern, Hamburger-Nav
- `docs/design/wireframes/iteration-12-redesign.md` — Party-Account Cards, Participant Dialog
- ADR-0019: Design-System Standardisierung

---

## Reihenfolge & Abhängigkeiten

```
Phase 0 (CSS + Logo + Nav)           ← Grundlage für alles
  ├── Phase 1A (Weightings Bug)       ← unabhängig, Quick Win
  ├── Phase 1B (Sortierung)           ← unabhängig, Quick Win
  ├── Phase 2 (Action-Spalten)        ← braucht 0B (Icon-CSS)
  │   ├── Phase 3 (Participants)      ← braucht 2 (Pattern)
  │   └── Phase 5 (Add/Create)        ← braucht 2 (Konsistenz)
  ├── Phase 4A (Namen statt Email)    ← unabhängig
  │   └── Phase 4B (Party Cards)      ← braucht 4A
  │       └── Phase 4C (Weight Dialog)← braucht 1A + 2
  └── Phase 6 (Landing + Logo)        ← braucht 0A (Logo)

Phase 7 (Tests) ← nach allen funktionalen Phasen
Phase 8 (Docs) ← am Ende
```

## Geschätzter Umfang
- ~50 Dateien betroffen (Templates, CSS, Controller, Tests, i18n, JPA Entity)
- 3 CSS-Dateien synchron halten (SCS-Architektur)
- 2 Bug-Fixes, 1 Event-Schema-Anpassung, ~15 Templates, ~6 Tests
