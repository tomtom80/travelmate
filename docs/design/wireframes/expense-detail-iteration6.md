# Wireframes: Expense Detail Page — Iteration 6

**Version**: Iteration 6 design (v0.7.0)
**Status**: Design specification — no code written
**Companion**: `docs/design/components/expense-iteration6.md`

---

## Page: `/expense/{tripId}` — OPEN state (with review queue)

```
+=========================================================+
|  [ Travelmate ]     Reisepartei  Reisen  Abmelden       |
+=========================================================+

  Sommerurlaub 2026
  Abrechnung — [ Offen ]

  +-------------------------------------------------------+
  |  Gesamtbetrag: 502,50 EUR                             |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Belege  [ 2 zu prüfen ]                              |
  |                                                       |
  |  Beschreibung  Betrag  Kategorie  Datum  Von  Status  Aktionen
  |  -----------------------------------------------------------
  |  Supermarkt    42,50   Lebensm.   12.07  Ali  GENEH.  —
  |  Hotel Alpin   320,00  Unterkunft 12.07  Bob  EINGR.  [✓ Gen.][✗ Abl.]
  |  Gondel        18,00   Aktivität  13.07  Ali  EINGR.  [✓ Gen.][✗ Abl.]
  |  Restaurant    82,00   Restaur.   13.07  Ali  ABGEL.  —
  |    ↳ Ablehnungsgrund: Beleg doppelt erfasst.          |
  |  Tankstelle    40,00   Transport  14.07  Bob  ENTWURF [Einreichen][Entf.]
  |  -----------------------------------------------------------
  |
  |  [ + Beleg hinzufügen ]                               |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Gewichtung                                           |
  |  -----------------------------------------------------------
  |  Alice Müller    [ 1.0 ] [Aktualisieren]              |
  |  Bob Meier       [ 1.0 ] [Aktualisieren]              |
  |  Clara Schmidt   [ 0.5 ] [Aktualisieren]              |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Saldo                                                |
  |  Teilnehmer         Betrag                            |
  |  Alice Müller       +125,00 EUR  (erhält)             |
  |  Bob Meier          −60,00 EUR   (schuldet)           |
  |  Clara Schmidt      −65,00 EUR   (schuldet)           |
  +-------------------------------------------------------+

  [ ! 2 Belege noch nicht geprüft. Bitte alle Belege prüfen,
    bevor die Abrechnung abgeschlossen werden kann. ]

  ← Zurück zur Reise

```

---

## Page: `/expense/{tripId}` — OPEN state, Rejection inline expansion

When organizer taps "Ablehnen" on "Hotel Alpin":

```
  +-------------------------------------------------------+
  |  Belege  [ 1 zu prüfen ]                              |
  |                                                       |
  |  Hotel Alpin   320,00  Unterkunft  12.07  Bob  EINGR. [✓ Gen.][✗ Abl.]
  |  +---------------------------------------------------+
  |  | Ablehnungsgrund:                                  |
  |  | [ Betrag stimmt nicht überein          ]          |
  |  | [ Abbrechen ]  [ Ablehnen bestätigen ] |
  |  +---------------------------------------------------+
  |  Gondel         18,00   Aktivität   13.07  Ali  EINGR. [✓][✗]
  +-------------------------------------------------------+
```

The inline rejection form appears as an additional `<tr>` below the receipt row. It is returned by `GET /{tid}/receipts/{rid}/reject-form` into `#reject-row-{rid}`.

---

## Page: `/expense/{tripId}` — OPEN state, Add Receipt Dialog (ACCOMMODATION selected)

```
  +--------------------------------------------+
  |  Beleg hinzufügen                      [X] |
  +--------------------------------------------+
  |  Beschreibung                              |
  |  [ Hotel Alpin                          ]  |
  |                                            |
  |  Kategorie                                 |
  |  [ Unterkunft                         ▼ ] |
  |                                            |
  |  [ Betrag         ]  [ Datum           ]   |
  |  [ 320.00         ]  [ 2026-07-12      ]   |
  |                                            |
  |  Bezahlt von                               |
  |  [ Bob Meier                          ▼ ] |
  |                                            |
  |  ── Unterkunft — Aufteilung nach Über- ──  |
  |     nachtungen                             |
  |                                            |
  |  Vorschau der Kostenaufteilung:            |
  |  Name           Nächte  Anteil   Betrag    |
  |  Alice Müller   5       50 %     160,00 €  |
  |  Bob Meier      5       50 %     160,00 €  |
  |  Clara Schmidt  —       (nicht anwesend)   |
  |                                            |
  |        [ Abbrechen ]  [ Beleg hinzufügen ] |
  +--------------------------------------------+
```

---

## Page: `/expense/{tripId}` — OPEN state, Settle blocked

```
  +------------------------------------------------------+
  |  [ ! ] 2 Beleg(e) noch nicht geprüft. Bitte alle    |
  |  Belege prüfen, bevor die Abrechnung abgeschlossen   |
  |  werden kann.                                        |
  +------------------------------------------------------+

  [ Abrechnung abschliessen — deaktiviert / nicht sichtbar ]
```

The settle button is either hidden or rendered as `disabled` attribute when `submittedCount > 0`. Hiding is preferred — a disabled button without explanation creates confusion. The warning paragraph takes its place.

---

## Page: `/expense/{tripId}` — OPEN state, Settle Confirmation Dialog

Triggered when organizer taps "Abrechnung abschliessen" (all receipts approved):

```
  +--------------------------------------------+
  |  Abrechnung abschliessen?             [X]  |
  +--------------------------------------------+
  |  Möchten Sie die Abrechnung wirklich       |
  |  abschliessen? Dies kann nicht rückgängig  |
  |  gemacht werden.                           |
  |                                            |
  |  Belege: 8   Gesamtbetrag: 502,50 EUR      |
  |                                            |
  |     [ Abbrechen ]  [ Abschliessen ]        |
  +--------------------------------------------+
```

---

## Page: `/expense/{tripId}` — SETTLED state (read-only)

```
+=========================================================+
|  [ Travelmate ]     Reisepartei  Reisen  Abmelden       |
+=========================================================+

  Sommerurlaub 2026
  Abrechnung — [ Abgerechnet ]

  +-------------------------------------------------------+
  |  Gesamtbetrag: 502,50 EUR                             |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Ausgaben nach Kategorie                              |
  |  -----------------------------------------------------------
  |  Kategorie       Betrag        Anteil                 |
  |  Unterkunft      320,00 EUR    64 %                   |
  |  Lebensmittel     42,50 EUR     8 %                   |
  |  Aktivität        18,00 EUR     4 %                   |
  |  Restaurant       82,00 EUR    16 %                   |
  |  Transport        40,00 EUR     8 %                   |
  |  -----------------------------------------------------------
  |  Gesamt          502,50 EUR   100 %                   |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Belege                                               |
  |  -----------------------------------------------------------
  |  Beschreibung   Betrag  Kategorie   Datum   Von       |
  |  Supermarkt     42,50   Lebensm.    12.07   Alice     |
  |  Hotel Alpin   320,00   Unterkunft  12.07   Bob       |
  |  Gondel         18,00   Aktivität   13.07   Alice     |
  |  Tankstelle     40,00   Transport   14.07   Bob       |
  |  Restaurant     82,00   Restaur.    13.07   Alice     |
  |  -----------------------------------------------------------
  |  (Abgelehnte Belege: 1 — nicht enthalten)             |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Saldo                                                |
  |  -----------------------------------------------------------
  |  Teilnehmer        Betrag                             |
  |  Alice Müller      +163,33 EUR  (erhält)              |
  |  Bob Meier         −98,33 EUR   (schuldet)            |
  |  Clara Schmidt     −65,00 EUR   (schuldet)            |
  +-------------------------------------------------------+

  +-------------------------------------------------------+
  |  Überweisungen                                        |
  |                                                       |
  |  Bob Meier    zahlt   Alice Müller    98,33 EUR       |
  |  Clara Schmidt  zahlt  Alice Müller   65,00 EUR       |
  +-------------------------------------------------------+

  ← Zurück zur Reise

```

---

## Mobile Layout: Receipt Cards (< 640px)

On mobile, the receipt table switches to a stacked card list. Each receipt is an `<article>` with a compact layout:

```
+------------------------------------------+
| Hotel Alpin                              |
| 320,00 EUR · Unterkunft · 12.07.2026     |
| Bezahlt von: Bob Meier                   |
|                     [ Eingereicht ]      |
| [ Genehmigen ]  [ Ablehnen ]             |
+------------------------------------------+

+------------------------------------------+
| Gondel-Fahrt                             |
| 18,00 EUR · Aktivität · 13.07.2026       |
| Bezahlt von: Alice Müller                |
|                     [ Abgelehnt (!) ]    |
| Grund: Doppelt erfasst.  [ Bearbeiten ]  |
+------------------------------------------+
```

The action buttons use full-width layout on mobile (`display: flex; flex-direction: column; gap: 0.5rem` on the actions container). The "Genehmigen" button should be visually distinct (primary colour) and "Ablehnen" secondary/outline to reduce risk of accidental tap confusion.

---

## Accessibility Notes

- All `<mark>` status badges have descriptive text — no icon-only badges
- `role="alert"` on inline feedback notices ensures screen reader announcement
- The inline rejection form is focusable: when the HTMX swap completes, focus should move to the first field (`hx-on::after-swap="document.querySelector('#rejection-reason-{rid}').focus()"`)
- The settlement summary transfer list uses `<ul>` (unordered, as transfers have no ordered significance)
- The category breakdown table has a `<caption>` element for screen readers
- The accommodation preview table has `<thead>` with proper `<th scope="col">` headers
- All dialogs: focus is trapped inside while open (native `<dialog>` behaviour); `aria-modal="true"` is set

---

## Responsive Breakpoints

| Breakpoint | Layout change |
|------------|--------------|
| > 768px | Table layout for receipts, full column set visible |
| 480px–768px | Table layout, "Kategorie" column hidden (low priority) |
| < 480px | Card layout for receipts, stacked form fields in dialog |

The dialog itself is `max-width: min(600px, 95vw)` to remain usable on any screen size.
