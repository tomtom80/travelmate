# Design System — Travelmate UI/UX

Stand: 2026-03-25 (Iteration 12 Redesign)

## Grundprinzipien

- **PicoCSS 2** als Basis (CDN) — semantisches HTML wird automatisch gestylt
- **HTMX 2.0.8** für dynamische Interaktionen — kein SPA/React
- **Inline SVG Icons** — keine Icon-Font-Libraries, Icons erben `currentColor`
- **Mobile-First** — Hamburger-Nav auf Mobile, Cards statt Tabellen wo sinnvoll

## Logo

Zwei SVG-Varianten in `static/images/`:
- `logo.svg` — Kompass-Mark + "travel**mate**" Wordmark (Desktop + Mobile Nav)
- `logo-mark.svg` — Nur Kompass-Mark (Favicon-Kandidat)

Farbe: `#3366CC` (--tm-primary)

## Navigation

```html
<nav class="container nav-bar">
    <ul>
        <li><a href="..." class="nav-brand"><img src="/images/logo.svg" class="nav-logo"></a></li>
        <li class="nav-toggle"><button onclick="...">☰</button></li>
    </ul>
    <ul class="nav-links">
        <li><a href="...">Link</a></li>
        <li><form><button class="nav-logout-btn">Logout</button></form></li>
    </ul>
</nav>
```

- Desktop: Logo + Links inline, Logout als Ghost-Button
- Mobile (≤767px): Logo links, Hamburger-Button rechts (blau), Dropdown-Panel
- Close: Outside-Click, Escape, oder erneuter Klick

## Icon Buttons (`.btn-icon`)

Einheitliches 2.25rem-Quadrat für alle Tabellenaktionen.

```html
<!-- Edit (Stift) -->
<button class="btn-icon" title="Bearbeiten">
    <svg viewBox="0 0 24 24"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/></svg>
</button>

<!-- Delete (X) — Danger-Variante -->
<button class="btn-icon btn-icon--danger" title="Löschen">
    <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
</button>

<!-- Checkmark (Annehmen) -->
<button class="btn-icon" title="Annehmen">
    <svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>
</button>
```

## Kebab-Menu (`.kebab-menu`)

Für sekundäre/seltene Aktionen — nutzt nativen `<details>` (kein JS-Library).

```html
<details class="kebab-menu">
    <summary title="Mehr">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/></svg>
    </summary>
    <div class="kebab-menu__panel">
        <button>Aktion 1</button>
        <button class="kebab-menu__danger">Gefährliche Aktion</button>
    </div>
</details>
```

## Aktionsspalte (`.action-col`)

Jede Tabelle mit Aktionen hat eine konsistente letzte Spalte.

```html
<th class="table-actions-header" th:text="#{common.actions}">Aktionen</th>
...
<td>
    <div class="action-col">
        <button class="btn-icon">...</button>
        <details class="kebab-menu">...</details>
    </div>
</td>
```

## Add/Create Toolbar (`.table-toolbar`)

Button zum Erstellen/Hinzufügen **vor** der Tabelle, rechtsbündig.

```html
<div class="table-toolbar">
    <a href="..." role="button" class="outline btn-add">Neu erstellen</a>
</div>
<figure class="table-cards">
    <table>...</table>
</figure>
```

## Dialog-Pattern

Für Formulare die nicht inline passen (z.B. Stay-Period-Bearbeitung).

```html
<button type="button" class="btn-icon"
        data-dialog-id="my-dialog-123"
        onclick="document.getElementById(this.dataset.dialogId).showModal()">
    <svg><!-- edit icon --></svg>
</button>

<dialog id="my-dialog-123">
    <article>
        <header>
            <button type="button" rel="prev" onclick="this.closest('dialog').close()">&times;</button>
            <h3>Titel</h3>
        </header>
        <form method="post" action="...">
            <!-- form fields -->
            <footer style="display:flex; gap:0.5rem; justify-content:flex-end;">
                <button type="button" class="secondary" onclick="this.closest('dialog').close()">Abbrechen</button>
                <button type="submit">Speichern</button>
            </footer>
        </form>
    </article>
</dialog>
```

Wichtig: Kein `th:onclick` mit Variablen verwenden (Thymeleaf XSS-Schutz blockiert String-Expressions in Event-Handlern). Stattdessen `data-*` Attribute + statisches `onclick`.

## Party-Account Cards

Statt 8-Spalten-Tabelle: Card-pro-Party mit klappbaren Details.

```
.party-account-card
├── .party-account-card__header     (Name + Balance)
├── .party-account-card__summary    (Flex-Grid: Anteil, Belege, Anzahlung)
├── .party-account-card__status     (Statustext)
├── <details> Mitglieder
└── <details> Kontoverlauf
```

## CSS-Variablen

```css
:root {
    --tm-danger: #dc2626;
    --tm-danger-bg: #fef2f2;
    --tm-danger-border: #fecaca;
    --tm-primary: #3366CC;
    --tm-primary-hover: #2952a3;
    --tm-icon-size: 2.25rem;
    --tm-surface-muted: #f8fafc;    /* nur Trips */
    --tm-border-strong: #d8dee9;    /* nur Trips */
    --tm-text-soft: #526071;        /* nur Trips */
}
```

## Sortierung

Alle JPA `@OneToMany` Collections MÜSSEN `@OrderBy` haben:
- Participants: `firstName ASC, lastName ASC`
- Receipts: `date ASC, description ASC`
- Weightings: `participantId ASC`
- AdvancePayments: `partyName ASC`
- MealPlan Slots: `date ASC, mealType ASC`
- Shopping Items: `source ASC, name ASC`
- Accommodation Rooms: `name ASC`
