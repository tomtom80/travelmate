# Iteration 15 — Refined User Stories: Unterkunftsabstimmung Redesign

**Date**: 2026-03-31
**Target Version**: v0.15.0
**Bounded Contexts**: Trips (AccommodationPoll aggregate, Accommodation aggregate, UI)

**Implementation Status**: REFINED

---

## Kontext und Motivation

Die Unterkunftsabstimmung (AccommodationPoll) ist mit Iteration 14 als technisches Fundament
implementiert. Die Praxis zeigt jedoch drei strukturelle Luecken:

1. **Fehlendes Buchungs-Workflow**: Nach der Bestaetigug gilt die Unterkunft als gebucht — ohne
   dass der Organisator sie tatsaechlich buchen muss. Schlaegt die Buchung beim Anbieter fehl,
   gibt es keine Moeglichkeit, die Abstimmung neu zu routen.

2. **Falsches Ausstattungsmodell**: Raeume haben ein Freitext-`features`-Feld, das Ausstattung
   (WiFi, Pool etc.) repraesentiert. Ausstattung gehoert semantisch zur Unterkunft (nicht zum
   Zimmer), und ein Freitext-Feld erzeugt keinen strukturierten Mehrwert. Ein standardisiertes
   `Amenity`-Enum erlaubt Filterung, Icons und sauberes i18n.

3. **UI-Qualitaet**: Das Erstellformular hat Layoutfehler (Zimmerzeilen laufen ueber), die
   Abstimmungsvisualisierung ist uneinheitlich, und die Mobilansicht wurde nicht entsprechend
   Iteration 11 nachgezogen.

Diese Iteration behebt alle drei Luecken. Sie ist bewusst auf den `AccommodationPoll` Aggregate
fokussiert — kein anderer Aggregate wird unveraendert angefasst.

---

## Epic-Zuordnung

Alle Stories gehoeren zu **E-TRIPS-07** (Location & Accommodation) und **E-TRIPS-08**
(Collaborative Trip Decision Making).

---

## Neue Fachbegriffe (Ubiquitous Language-Erweiterung)

| UI (DE) | UI (EN) | Code | Kontext |
|---------|---------|------|---------|
| Buchungsversuch | Booking Attempt | BookingAttempt | Trips — ein Versuch, einen Kandidaten zu buchen |
| Ausstattung | Amenity | Amenity | Trips — standardisiertes Merkmal einer Unterkunft |
| Buchungsfehlschlag | Booking Failure | BookingFailure | Trips — fehlgeschlagener Buchungsversuch mit Notiz |

---

## Dependency Graph

```
S15-A: Ausstattungsmodell korrigieren (Amenity-Enum)
  — Voraussetzung fuer alle weiteren Stories
  — Datenmigration: features-Spalte auf Kandidaten-Ebene; Raeume behalten nur Kerndaten

S15-B: Buchungs-Workflow (BookingAttempt)
  — setzt S15-A voraus (sauberes Kandidatenmodell)
  — neues AccommodationPollStatus-Zustand: AWAITING_BOOKING
  — fuehrt BookingAttempt-Entity in AccommodationPoll ein

S15-C: Fallback bei Buchungsfehlschlag
  — setzt S15-B voraus (BookingAttempt muss existieren, um als fehlgeschlagen markiert zu werden)
  — definiert die Rueckkopplungsschleife (poll reopens oder Kandidaten ergaenzen)

S15-D: UI-Redesign Unterkunftsabstimmung
  — setzt S15-A, S15-B, S15-C voraus (finales Datenmodell muss stehen)
  — reine Frontend-Aenderung; keine Domaenenlogik
```

---

## Empfohlener Iterations-Scope

| ID | Story | Prioritaet | Groesse | Bounded Context |
|----|-------|------------|---------|-----------------|
| S15-A | Ausstattungsmodell korrigieren | Must | M | Trips |
| S15-B | Buchungsbestaetigung nach Abstimmung | Must | L | Trips |
| S15-C | Fallback bei Buchungsfehlschlag | Must | M | Trips |
| S15-D | UI-Redesign Unterkunftsabstimmung | Should | L | Trips |

**Scope-Begruendung:**

S15-A ist eine Voraussetzung fuer alles andere: Das Ausstattungsmodell muss korrigiert werden,
bevor das Formular redesignt wird (S15-D) und bevor neue Kandidaten korrekt gespeichert werden.
Die Migration ist nicht-destruktiv — bestehende `features`-Texte koennen als einzige
`UNCLASSIFIED`-Amenity oder als leere Liste migriert werden.

S15-B und S15-C bilden zusammen den vollstaendigen Buchungs-Workflow. Ohne S15-C waere S15-B
sinnlos (eine Buchung, die nie fehlschlagen kann, loest das Problem nicht). Beide muessen in
derselben Iteration ausgeliefert werden.

S15-D ist ein Should, weil die Abstimmung nach S15-A/B/C funktional korrekt ist. Das Redesign
verbessert Bedienbarkeit und Mobilansicht erheblich, aber ein Rollout ohne S15-D ist moeglich.

---

## Empfohlene Implementierungsreihenfolge

| Schritt | Story | Begruendung |
|---------|-------|-------------|
| 1 | S15-A | Domaenenmodell und Migration zuerst; alle anderen Stories bauen darauf auf |
| 2 | S15-B | Buchungs-Workflow: neuer Zustand + BookingAttempt-Entity + Service |
| 3 | S15-C | Fehlschlag-Logik: Rueckkopplungsschleife und neue Kandidaten-Erweiterung |
| 4 | S15-D | UI-Redesign auf Basis des finalisierten Modells |

---

## Story S15-A: Ausstattungsmodell korrigieren

**Epic**: E-TRIPS-07
**Priority**: Must
**Size**: M
**User Story**:
**Als** Reiseteilnehmer **moechte ich** beim Vorschlagen einer Unterkunft aus einer standardisierten
Ausstattungsliste (WLAN, Pool, Kueche ...) waehlen, **damit** alle Teilnehmer die Ausstattung
auf einen Blick erkennen und Kandidaten vergleichen koennen.

### Acceptance Criteria

- **Given** ich fuege einen Unterkunftskandidaten hinzu, **When** ich das Formular ausfuelle,
  **Then** kann ich aus einer standardisierten Ausstattungsliste (Checkboxen) beliebig viele
  Merkmale auswaehlen; das `features`-Freitext-Feld existiert nicht mehr.

- **Given** ein Kandidat hat die Merkmale `WiFi` und `Pool`, **When** ein Teilnehmer die
  Abstimmungsuebersicht oeffnet, **Then** werden die Merkmale als Icons mit Label dargestellt
  (nicht als Freitext).

- **Given** ein Zimmer eines Kandidaten, **When** es angezeigt wird, **Then** hat es nur noch
  `name`, `bedCount`, `bedDescription` (optional) und `pricePerNight` (optional) — keine
  `features`-Spalte mehr.

- **Given** bestehende Kandidaten mit gefuelltem `features`-Text, **When** die Datenmigration
  laeuft, **Then** werden die Ausstattungsfelder leer migriert (leere Liste); bestehende Daten
  gehen nicht verloren, aber `features` wird nicht automatisch in Amenities uebersetzt.

- **Given** kein Merkmal ist ausgewaehlt, **When** der Kandidat gespeichert wird, **Then** ist
  das zulaessig — Ausstattung ist optional.

### Technical Notes

- Bounded Context: Trips
- Neues Value Object: `Amenity` (Enum) im Package `domain/accommodationpoll/`
  ```
  WiFi, Pool, Kitchen, Parking, Garden, WashingMachine, AirConditioning,
  Pets, Sauna, Fireplace, Dishwasher, TV, Balcony
  ```
- `AccommodationCandidate` erhaelt `Set<Amenity> amenities` (ersetzt das `features`-Feld auf
  Zimmer-Ebene); `CandidateRoom` verliert das `features`-Feld, erhaelt `bedDescription`
  (optional, free text)
- `CandidateRoom`-Record-Signatur nach Migration:
  `record CandidateRoom(String name, int bedCount, String bedDescription, BigDecimal pricePerNight)`
- Flyway V20 in Trips SCS:
  - neue Tabelle `accommodation_candidate_amenity(candidate_id UUID, amenity VARCHAR(50))`
  - Spalte `features` auf `accommodation_candidate_room` (falls separate Tabelle existiert) oder
    auf `accommodation_candidate` wird geleert / entfernt
  - Hinweis: V19 legte keine `accommodation_candidate_room`-Tabelle an — Raeume wurden bisher
    als JSON-Blob gespeichert oder per JPA `@ElementCollection`; vor Implementierung pruefen
    welche Persistence-Strategie tatsaechlich im Einsatz ist (siehe `AccommodationCandidateJpaEntity`)
- Commands: `ProposeCandidateCommand` und `AddAccommodationCandidateCommand` erhalten
  `Set<Amenity> amenities` statt `String features`
- `AccommodationPollRepresentation.CandidateRepresentation` erhaelt `Set<Amenity> amenities`
- i18n: Schluesselprafix `amenity.{ENUM_NAME}` fuer DE/EN; Icon-Map in CSS via `data-amenity`-
  Attribut (SVG-Sprite oder Unicode-Fallback)
- Bestehende Tests: `CandidateRoom`-Konstruktor-Aufrufe mit `features` muessen angepasst werden;
  `AccommodationPollService.mapRooms()` prueft nicht mehr auf nicht-leere `features`

---

## Story S15-B: Buchungsbestaetigung nach Abstimmung

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**User Story**:
**Als** Organisator **moechte ich** nach dem Bestaetigen des Abstimmungsgewinners einen
Buchungsversuch registrieren koennen, **damit** der Gruppe klar ist, ob die Unterkunft tatsaechlich
gebucht wurde oder ob noch ein externer Schritt aussteht.

### Acceptance Criteria

- **Given** die `AccommodationPoll` ist OPEN und ich waehle einen Kandidaten als Gewinner,
  **When** ich die Auswahl bestatige, **Then** wechselt der Poll-Status zu `AWAITING_BOOKING`,
  der ausgewaehlte Kandidat erhaelt den Status `SELECTED`, und es wird _noch kein_
  `Accommodation`-Aggregat erzeugt.

- **Given** der Poll-Status ist `AWAITING_BOOKING`, **When** ich als Organisator die Buchung
  als erfolgreich markiere, **Then** wechselt der Status zu `BOOKED`, ein `Accommodation`-
  Aggregat wird aus den Kandidatendaten erzeugt, und alle nicht-gewaehlten ACTIVE-Kandidaten
  erhalten den Status `ARCHIVED`.

- **Given** der Poll-Status ist `AWAITING_BOOKING`, **When** ein Teilnehmer (nicht Organisator)
  die Poll-Seite aufruft, **Then** sieht er den Status "Buchung ausstehend" und den Namen des
  ausgewaehlten Kandidaten; er hat keine Schaltflaeche zum Bestaetigen oder Ablehnen.

- **Given** der Poll-Status ist `BOOKED`, **When** jemand die Abstimmungsseite aufruft,
  **Then** wird die gebuchte Unterkunft mit Name, URL und Ausstattung hervorgehoben; die
  historische Stimmverteilung bleibt als schreibgeschuetztes Archiv sichtbar.

- **Given** der Poll-Status ist `BOOKED` oder `CANCELLED`, **When** ich versuche erneut
  zu bestaetigen oder abzubrechen, **Then** lehnt das System die Aktion ab.

- **Given** ein erster `BookingAttempt` ist registriert, **When** der Organisator die
  erfolgreiche Buchung bestaetigt, **Then** wird der `BookingAttempt` mit `SUCCESS` und
  Zeitstempel abgeschlossen.

### Technical Notes

- Bounded Context: Trips
- Zustandserweiterung `AccommodationPollStatus`:
  - alter Zustand `CONFIRMED` wird zu `AWAITING_BOOKING` umbenannt (Breaking Change in Flyway-
    Enum-Constraints; V21 migriert `'CONFIRMED'` → `'AWAITING_BOOKING'`)
  - neuer Zustand `BOOKED` ersetzt die bisherige Auto-Erstellung beim `confirm()`-Aufruf
- Neue Entity `BookingAttempt` in `domain/accommodationpoll/`:
  ```java
  // entity within AccommodationPoll
  BookingAttemptId, AccommodationCandidateId candidateId, BookingAttemptStatus status,
  String notes, Instant attemptedAt, Instant resolvedAt
  ```
  `BookingAttemptStatus`: `PENDING`, `SUCCESS`, `FAILED`
- Commands:
  - `SelectAccommodationCandidateCommand(tenantId, pollId, candidateId)` — loest OPEN→AWAITING_BOOKING aus
  - `RecordBookingSuccessCommand(tenantId, pollId, bookingAttemptId)` — loest AWAITING_BOOKING→BOOKED aus
- Events (BC-intern):
  - `AccommodationCandidateSelected(pollId, candidateId, occurredOn)`
  - `AccommodationBooked(pollId, candidateId, tenantId, tripId, occurredOn)` — loest Accommodation-
    Erstellung im selben Transaction aus (wie Policy P2 in S14-E)
- Umbenennung im Controller:
  - `/confirm` Endpunkt wird zu `/select` fuer OPEN→AWAITING_BOOKING
  - neuer POST `/book` Endpunkt fuer AWAITING_BOOKING→BOOKED
- `AccommodationPollService.confirmPoll()` wird zu `selectCandidate()` (kein Accommodation-Aggregat
  mehr erzeugt); neue Methode `recordBookingSuccess()` erzeugt das Accommodation-Aggregat
- Flyway V21: ENUM-Migration + neue Spalten/Tabelle fuer `BookingAttempt`
- `CandidateStatus`-Enum: `ACTIVE`, `SELECTED`, `ARCHIVED` (neu) — ersetzt implizite Logik
  durch explizites Statusfeld auf `accommodation_candidate.candidate_status`

---

## Story S15-C: Fallback bei Buchungsfehlschlag

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: M
**User Story**:
**Als** Organisator **moechte ich** einen Buchungsfehlschlag mit einer optionalen Notiz
registrieren koennen, **damit** die Gruppe den naechstbesten Kandidaten auswaehlen kann,
ohne die gesamte Abstimmung neu starten zu muessen.

### Acceptance Criteria

- **Given** der Poll-Status ist `AWAITING_BOOKING`, **When** ich die Buchung als fehlgeschlagen
  markiere (optional mit Notiz), **Then** wechselt der Status zurueck zu `OPEN`, der vorherige
  Gewinnerkandidat erhaelt den Status `BOOKING_FAILED`, er taucht nicht mehr in der aktiven
  Kandidatenliste auf, und alle anderen Kandidaten sind weiterhin wahlbar.

- **Given** der Fehlschlag-Kandidat hat `BOOKING_FAILED`, **When** ein Teilnehmer die
  Abstimmung oeffnet, **Then** sieht er den Kandidaten in einem separaten "Fehlgeschlagen"-
  Abschnitt mit dem optionalen Fehlertext; er kann ihn nicht erneut bewaehlen.

- **Given** alle verbleibenden Kandidaten haben ebenfalls `BOOKING_FAILED` oder es existieren
  keine weiteren aktiven Kandidaten, **When** die Poll in den Status `OPEN` zurueckkehrt,
  **Then** zeigt das System dem Organisator einen Hinweis: "Keine weiteren Kandidaten vorhanden
  — Kandidaten ergaenzen oder Abstimmung abbrechen."

- **Given** der Organisator moechte nach einem Fehlschlag neue Kandidaten hinzufuegen,
  **When** er einen neuen Kandidaten erstellt, **Then** wird dieser direkt als `ACTIVE` angelegt
  und nimmt an der Abstimmung teil; alle bestehenden Stimmen bleiben erhalten.

- **Given** ich bin ein Teilnehmer mit einer Stimme auf dem Fehlschlag-Kandidaten,
  **When** die Poll in `OPEN` zurueckkehrt, **Then** ist meine Stimme entfernt und ich muss
  neu abstimmen.

- **Given** mehrere aufeinanderfolgende Buchungsversuche fuer denselben oder verschiedene
  Kandidaten, **When** der Organisator die Historie aufruft, **Then** sind alle `BookingAttempt`-
  Eintraege mit Status, Zeitstempel und Notiz chronologisch sichtbar.

### Technical Notes

- Bounded Context: Trips
- Neuer `CandidateStatus`-Wert: `BOOKING_FAILED` (zusaetzlich zu `ACTIVE`, `SELECTED`, `ARCHIVED`)
- Flyway V22: `accommodation_candidate.candidate_status` erhaelt `BOOKING_FAILED` als gueltigen Wert;
  neue Spalten `booking_failure_notes VARCHAR(1000)` auf `booking_attempt`-Tabelle (bereits in V21)
- Aggregat-Methode: `AccommodationPoll.recordBookingFailure(BookingAttemptId, String notes)`:
  1. Setzt den `BookingAttempt` auf `FAILED`
  2. Setzt den `SELECTED`-Kandidaten auf `BOOKING_FAILED`
  3. Entfernt alle Stimmen fuer diesen Kandidaten (Waehlerstimmen werden ungueltig)
  4. Setzt Poll-Status zurueck auf `OPEN`
- Command: `RecordBookingFailureCommand(tenantId, pollId, bookingAttemptId, notes)`
- Invariante: die Aggregat-Methode prueft, dass genau ein Kandidat `SELECTED` ist, wenn der
  Fehlschlag registriert wird; andernfalls Fehler
- Leere-Kandidaten-Pruefung: `AccommodationPollService.recordBookingFailure()` prueft nach der
  Aggregat-Operation, ob noch mindestens ein `ACTIVE`-Kandidat existiert; falls nicht, wird
  ein `AccommodationPollEmptyAfterFailureEvent` (BC-intern) registriert, den der Controller
  als Warnung in der UI darstellt
- Controller: neuer POST-Endpunkt
  `/{tripId}/accommodationpoll/{pollId}/booking-attempts/{attemptId}/fail`
- Stimmen-Invalidierung ist Verantwortung des Aggregats, nicht des Services; das Aggregat
  iteriert `votes` und entfernt alle Eintraege mit `selectedCandidateId == failedCandidateId`

---

## Story S15-D: UI-Redesign Unterkunftsabstimmung

**Epic**: E-TRIPS-07 / E-TRIPS-08
**Priority**: Should
**Size**: L
**User Story**:
**Als** Reiseteilnehmer **moechte ich** eine uebersichtliche, mobile-freundliche
Unterkunftsabstimmungsseite, **damit** ich Kandidaten schnell vergleichen, meine Stimme
abgeben und den Buchungsstatus verfolgen kann — ohne durch Layoutfehler oder unklare
Aktionsreihenfolge verwirrt zu werden.

### Acceptance Criteria

- **Given** das Erstellformular wird geoeffnet, **When** ich Zimmerzeilen hinzufuege,
  **Then** bleibt das Layout korrekt (keine horizontalen Ueberlaeufe); jede Zimmerzeile hat
  `name`, `Anzahl Betten`, `Beschreibung` (optional) und `Preis/Nacht` (optional) als
  getrennte Eingabefelder.

- **Given** das Erstell- oder Kandidaten-Hinzufuegen-Formular, **When** ich es ausfuelle,
  **Then** gibt es eine Ausstattungs-Checklist (Checkboxen fuer alle `Amenity`-Werte) mit
  Icon und Label pro Merkmal.

- **Given** die Abstimmungsuebersicht mit mindestens einem Kandidaten, **When** ein Teilnehmer
  die Seite aufruft, **Then** ist jeder Kandidat als eigenstaendige Karte (`.card-grid`-
  Muster aus Iteration 11) dargestellt mit: Name, URL-Link (falls vorhanden), Ausstattungs-
  Icons, Zimmeranzahl, Stimmbalken und Waehlen-Schaltflaeche.

- **Given** es gibt Stimmen fuer Kandidaten, **When** die Abstimmungsergebnisse angezeigt
  werden, **Then** werden diese als farbige Balken (CSS-inline-`width`) dargestellt; der
  fuehrende Kandidat hebt sich visuell ab; die Prozentangabe ist korrekt gerundet.

- **Given** ein Kandidat hat eine Adresse, **When** er in der Karte angezeigt wird,
  **Then** gibt es einen "Auf Karte anzeigen"-Link, der `https://maps.google.com/?q=<address>`
  in einem neuen Tab oeffnet.

- **Given** ich bin auf einem Mobilgeraet (Breakpoint <= 767px), **When** ich die
  Abstimmungsseite oeffne, **Then** stapeln sich Kandidatenkarten vertikal ohne horizontalen
  Scroll; Schaltflaechen haben mindestens 44px Touch-Zielhoehe; das Erstellformular ist
  vollstaendig ohne Zoom bedienbar.

- **Given** der Status ist `AWAITING_BOOKING`, **When** ein Teilnehmer die Seite aufruft,
  **Then** ist ein deutliches Status-Banner sichtbar ("Buchung wird vorbereitet — Kandidat:
  [Name]"); der Organisator sieht zusaetzlich Schaltflaechen "Buchung bestaetigen" und
  "Buchung fehlgeschlagen".

- **Given** Kandidaten mit Status `BOOKING_FAILED`, **When** die Seite angezeigt wird,
  **Then** sind diese in einem separaten ausgeklappbaren Abschnitt "Fehlgeschlagene
  Buchungsversuche" unterhalb der aktiven Kandidaten; sie sind visuell gedaempft (niedrigerer
  Kontrast).

### Technical Notes

- Bounded Context: Trips
- Kein neues Domaenenmodell — reine Template- und CSS-Aenderung auf Basis von S15-A/B/C
- Template `accommodationpoll/overview.html` wird vollstaendig ueberarbeitet:
  - Ersetze `<table>`-basierte Kandidatenliste durch `.card-grid` mit `<article
    class="candidate-card">` pro Kandidat
  - Jede Karte: `.candidate-card__header` (Name + Waehlen-Radio + Stimmanzahl), `.candidate-
    card__amenities` (Icon-Reihe), `.candidate-card__rooms` (kompakte Zimmerliste),
    `.candidate-card__footer` (URL-Link + Maps-Link)
  - Balkendiagramm bleibt als separates `<article class="poll-chart">`, wird aber farbcodiert:
    CSS-Klassen `.bar--leading`, `.bar--normal`
- Template `accommodationpoll/create.html`:
  - Zimmerzeilen als `<fieldset class="room-fieldset">` (kein Flex-Overflow mehr)
  - Ausstattungs-Checkboxen als `<label class="amenity-checkbox">` in `<div class="amenity-grid">`
  - JavaScript-Logik fuer Zimmer-Hinzufuegen bleibt, aber Serialisierung umgestellt auf
    FormData (kein JSON-Blob per Hidden-Input fuer Rooms mehr — setzt Refactoring im Controller
    voraus, der einzelne Zimmerfelder als Listen entgegennimmt statt JSON)
- Neues Partial: `accommodationpoll/candidate-card.html` fuer HTMX-Fragment-Austausch beim
  Abstimmen (hx-target="closest article.candidate-card", hx-swap="outerHTML")
- Status-Banner fuer `AWAITING_BOOKING`: `<div class="booking-status-banner booking-status-
  banner--pending">` mit Kandidatenname und Schaltflaechen nur fuer Organisator
- Booking-Failure-Abschnitt: `<details class="booking-failed-section"><summary>...</summary>`
  — CSS-only, kein JavaScript
- CSS-Erweiterungen in der SCS-spezifischen CSS-Datei:
  `.candidate-card`, `.amenity-grid`, `.amenity-checkbox`, `.booking-status-banner`,
  `.booking-failed-section`, `.bar--leading`
- Maps-Link: `th:href="'https://maps.google.com/?q=' + ${#strings.replace(candidate.address(), ' ', '+')}"`
  nur rendern wenn `candidate.address() != null`
- Kein HTMX-Polling — Stimmabgabe per HTMX POST, Kandidatenkarte wird per `hx-swap="outerHTML"`
  aktualisiert (Stueckchen-Refresh der betroffenen Karte)

---

## Open Design Questions

### OD-1: CandidateStatus-Persistenz

Die Iteration 14 V19-Migration hat keine `candidate_status`-Spalte angelegt. Der Status `ACTIVE`
/ `ARCHIVED` / `SELECTED` ist derzeit implizit (ein Kandidat ist "selected" wenn
`selected_candidate_id` auf ihn zeigt). S15-B fuehrt einen expliziten `CandidateStatus` ein.

**Entscheidung ausstehend**: Soll `candidate_status` als Spalte auf `accommodation_candidate`
in V21 explizit gemacht werden (empfohlen: ja — ermoeglicht S15-C ohne JOIN auf
`accommodation_poll.selected_candidate_id`)?

### OD-2: Migration bestehender features-Texte

Bestehende `CandidateRoom.features`-Texte (Freitext) koennen nicht automatisch in `Amenity`-
Enum-Werte uebersetzt werden. Die Migration in V20 kann entweder:

a) die `features`-Spalte mit einem NULL-Wert stehen lassen und das alte Feld aus dem Aggregat
   entfernen (Datenverlust fuer bestehende Eintraege), oder
b) `features` als Legacy-Spalte beibehalten und in der UI als Hinweis anzeigen
   ("Alte Notiz: [features-Text]"), bis der Nutzer den Kandidaten bearbeitet.

**Empfehlung**: Option (a) — die Produktivdaten sind im Entwicklungsstadium; sauberer Schnitt
ist wertvoller als Migration von Testdaten.

### OD-3: Zimmer-Formular-Serialisierung

Das bestehende Create-Formular serialisiert Zimmerzeilen als JSON-Blob in einem Hidden-Input
(`roomsJson`). S15-D empfiehlt, auf Standard-HTML-Listen-Input umzusteigen
(`name="roomName[]"`, `name="roomBedCount[]"`), was eine Aenderung im Controller erfordert.

**Entscheidung ausstehend**: Soll S15-D den JSON-Blob-Ansatz beibehalten (weniger Risiko) oder
auf Standard-Listen-Input umstellen (sauberer, aber Koordination mit Controller noetig)?

**Empfehlung**: Umstellen auf Standard-Listen-Input — JSON-Blobs in Formularen sind fragil und
erschweren zukuenftige Server-Side-Validierung.

---

## Abgrenzung zu Iteration 14

Die folgenden S14-Aspekte werden in dieser Iteration _nicht_ veraendert:

- `DatePoll`-Aggregat und alle S14-A/B/C-Stories bleiben unveraendert
- `AccommodationPoll.create()` mit Mindest-2-Kandidaten-Invariante bleibt erhalten
- Die URL-Import-Integration (HS-4 aus S14-D) wird nicht veraendert — der Import laeuft
  weiterhin durch `proposeFromImport()` wenn eine Poll OPEN ist
- Die Beschraenkung auf PLANNING-Status fuer aktive Polls bleibt unveraendert
- `Trip.cancelTrip()` schliesst offene Polls (Policy P3) — diese Logik wird nicht veraendert

---

## Flyway-Migrationsplan

| Version | Inhalt |
|---------|--------|
| V20 | `accommodation_candidate_amenity(candidate_id, amenity)` — neue Tabelle; `features`-Spalte auf Zimmer-Ebene entfernen/leeren (je nach OD-2 Entscheidung) |
| V21 | `candidate_status` Spalte auf `accommodation_candidate`; `booking_attempt` Tabelle; ENUM-Migration `CONFIRMED` → `AWAITING_BOOKING` auf `accommodation_poll.status` |
| V22 | `booking_failure_notes` auf `booking_attempt` (falls nicht bereits in V21); `BOOKING_FAILED` als gueltiger `candidate_status`-Wert |

**Hinweis:** Vor Implementierung pruefen, ob `accommodation_candidate_room` als separate Tabelle
oder als `@ElementCollection` mit JSON-Blob implementiert ist (in `AccommodationCandidateJpaEntity`),
da dies die V20-Migration direkt beeinflusst.
