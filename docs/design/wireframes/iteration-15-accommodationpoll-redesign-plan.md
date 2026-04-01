# Iteration 15 — UX, Architektur und Requirements Plan fuer das AccommodationPoll Redesign

**Stand**: 2026-03-31
**Perspektiven**: UX Design, Architektur, Requirements Engineering
**Scope**: `accommodationpoll/create.html`, `accommodationpoll/overview.html`, zugehoerige
Representation-, Controller- und CSS-Vertraege im Trips-SCS
**Input**: [iteration-15-stories.md](../../backlog/iteration-15-stories.md),
[iteration-15-plan.md](../../backlog/iteration-15-plan.md),
[iteration-14-accommodationpoll-ux.md](./iteration-14-accommodationpoll-ux.md),
[design-system.md](../components/design-system.md)

---

## 1. Zielbild

Iteration 15 soll die Unterkunftsabstimmung von einer technisch funktionierenden Poll-Seite zu
einem belastbaren, mobil gut nutzbaren Planungswerkzeug weiterentwickeln.

Das Redesign verfolgt drei gleichwertige Ziele:

1. **Mobil zuerst**: Die Seite muss auf kleinen Smartphones vollstaendig ohne Horizontal-Scroll,
   Zoom oder ueberlaufende Formreihen funktionieren.
2. **Lesbarkeit und Ruhe**: Kandidaten muessen schnell vergleichbar sein, ohne dass Nutzer lange
   Tabellen oder Roh-URLs entschluesseln muessen.
3. **Realistischer Workflow**: "Ausgewaehlt" darf nicht mit "gebucht" verwechselt werden. Der
   externe Buchungsschritt und sein Fehlschlag muessen sichtbar und sicher gefuehrt sein.

---

## 2. UX-Leitprinzipien

### 2.1 Mobile-first statt Desktop-Downscale

- Primarer Entwurfspunkt ist `375px` Breite.
- Desktop erweitert den Informationsraum, ersetzt aber nicht die mobile Informationshierarchie.
- Aktionen liegen auf Mobile unter dem jeweiligen Inhalt, nie in engen Tabellenzellen.
- Primare Buttons sind auf Mobile full-width oder mindestens in klaren vertikalen Action-Stacks.

### 2.2 Vergleichbarkeit vor Dekoration

- Die wichtigste Frage lautet: "Welche Unterkunft passt fuer unsere Gruppe am besten?"
- Deshalb sind Rangfolge, Ausstattung, Schlafkapazitaet und Status wichtiger als dekorative
  Visuals.
- Horizontale Balken bleiben die primaere Abstimmungsvisualisierung.
- KPI-Kacheln duerfen Orientierung geben, aber nicht den eigentlichen Entscheidungsraum ueberlagern.

### 2.3 Eine Karte = eine Entscheidungseinheit

- Jeder Kandidat wird als eigenstaendige Karte mit immer gleicher Reihenfolge dargestellt:
  Name, Lage, Ausstattung, Zimmer, Stimmen, Aktion.
- Dadurch entsteht auf Mobile wie Desktop ein stabiles Scan-Muster.

### 2.4 Progressive Enhancement

- Keine eingebetteten Drittanbieter-Widgets.
- Keine Karten-Iframes.
- Externe Listings und Maps werden ueber normale Links in neuem Tab geoeffnet.
- HTMX nur dort, wo ein echter UX-Gewinn entsteht; PRG bleibt fuer groessere Statuswechsel okay.

---

## 3. Nutzer, Nutzungskontext und Hauptaufgaben

### 3.1 Organisator

Kontext:

- plant oft abends auf dem Sofa am Handy oder tagsueber am Desktop
- sammelt 2 bis 6 Vorschlaege
- muss nach der Gruppenentscheidung extern buchen

Top Tasks:

1. Unterkunftsvorschlaege anlegen oder importieren
2. Kandidaten fuer die Gruppe vergleichbar praesentieren
3. Gewinner auswaehlen
4. Buchungserfolg oder Buchungsfehlschlag sauber rueckmelden

### 3.2 Teilnehmer

Kontext:

- oeffnet die Abstimmung meist kurz mobil
- will schnell erkennen, worin sich die Optionen unterscheiden
- will mit wenig Aufwand abstimmen oder den Status verstehen

Top Tasks:

1. Optionen verstehen
2. fuer einen Kandidaten stimmen
3. den aktuellen Planungsstand erkennen

---

## 4. Hauptprobleme im aktuellen Zustand

### 4.1 Mobile Form-Brueche

- Zimmerzeilen laufen ueber
- Feldgruppen sind nicht sauber gegliedert
- Touch-Ziele und Abstaende sind inkonsistent

### 4.2 Schlechte Scanbarkeit im Overview

- Tabellenzellen enthalten gemischte Rohtexte
- URLs dominieren visuell den eigentlichen Kandidaten
- Rauminformationen sind nicht als Schlafkapazitaet lesbar

### 4.3 Unklarer Planungsstatus

- Nutzer koennen nicht schnell unterscheiden zwischen:
  - Poll offen
  - Gewinner ausgewaehlt
  - Buchung ausstehend
  - Buchung fehlgeschlagen
  - Unterkunft erfolgreich gebucht

### 4.4 Falsche semantische Gruppierung

- Amenities sind auf Zimmern statt auf der Unterkunft
- dadurch wird Dateneingabe laestiger und Darstellung unruhig

---

## 5. Informationsarchitektur der neuen Poll-Seite

### 5.1 Mobile Reihenfolge

Die Seite soll auf Mobile in dieser Reihenfolge aufgebaut sein:

1. `hgroup` mit Seitenkontext und Ruecksprung zur Reise
2. `poll-hero` mit Status, kurzem Erklaertext und Schrittbezug
3. `poll-kpi-grid` mit vier kompakten Statuskacheln
4. Status-Banner, falls `AWAITING_BOOKING`, `BOOKED` oder letzter Fehlschlag vorliegt
5. Ergebnisbereich mit horizontalen Balken
6. Kandidatenkarten
7. Organisator-Aktionen: Kandidat hinzufuegen, URL importieren, ggf. Auswahl/Buchung
8. Fehlgeschlagene Buchungsversuche als aufklappbarer Verlauf

Warum diese Reihenfolge:

- zuerst Orientierung
- dann Status
- dann Vergleich
- erst danach Bearbeitungsaktionen

### 5.2 Desktop Reihenfolge

Desktop nutzt dieselbe Reihenfolge, erweitert aber zwei Bereiche:

- KPI-Kacheln koennen in einer Viererreihe liegen
- Ergebnisbereich und Kandidaten koennen in einer asymmetrischen 1/3 zu 2/3-Aufteilung stehen,
  solange Mobile-first HTML-Struktur erhalten bleibt

---

## 6. Visuelles Konzept

### 6.1 Tonalitaet

Travelmate soll nicht wie ein generisches Admin-Backend wirken. Fuer Iteration 15 gilt:

- helle Oberflaeche
- starke, aber ruhige Akzentfarben
- klare Schatten nur fuer inhaltliche Gruppierung
- keine ueberfluessigen Farbwechsel pro Unterelement

### 6.2 Lesbarkeit

- Kandidatenname: groesster Textblock in der Karte
- Sekundaerinfos: Lagezeile und Beschreibung in reduzierter, aber kontrastreicher Tonalitaet
- amenities als kompakte Chips mit Icon plus Label
- Zimmer nicht als Satz, sondern als kleine Kapazitaets-Bausteine

### 6.3 Poll-Ergebnisvisualisierung

- horizontale Balken pro Kandidat
- gleiche Kandidatenfarbe in Balken und optionalem Punkt/Badge
- Fuehrung ueber Rangfolge, Prozentwert und Label
- kein Radar-Chart als primaeres Element

Begruendung:

- Reisegruppen muessen praezise vergleichen koennen
- horizontale Balken sind auf Mobile und Desktop zugleich am leichtesten interpretierbar

---

## 7. Mobile-first Komponentenplan

### 7.1 KPI-Kacheln

Inhalt:

- Kandidaten
- Stimmen
- Fuehrender Kandidat
- Status

UX-Regeln:

- auf Mobile 2x2 oder 1-spaltig, je nach Platz
- Zahlen gross, Labels klein
- Status nie nur als Farbe, immer auch als Text

### 7.2 Kandidatenkarte

Pflichtbereiche je Karte:

1. Titelzeile mit Name und externer Listing-Aktion
2. Lagezeile mit Adresse oder Ortsfragment und "Auf Karte anzeigen"
3. Amenity-Zeile
4. Zimmer-/Schlafbereich
5. optionale Kurzbeschreibung
6. Stimmenbereich
7. Aktion: abstimmen oder Statushinweis

Mobile-Regeln:

- Karte hat vertikalen Aufbau
- alle Meta-Informationen umbrechen sauber
- CTA immer im unteren Kartenbereich

Desktop-Regeln:

- gleiche Reihenfolge
- mehr Informationen duerfen nebeneinander laufen, aber nicht die Grundstruktur aendern

### 7.3 Create-/Add-Candidate-Form

Struktur pro Kandidatenblock:

1. Basisdaten: Name, URL, Beschreibung
2. Amenities als Checkbox-Grid
3. Zimmerbereich mit wiederholbaren `room-fieldset`
4. Block-Aktionen: Zimmer hinzufuegen, Kandidat entfernen

Mobile-Regeln:

- einspaltige Anordnung
- Zimmer-Fieldsets stapeln intern logisch
- Buttons gross genug fuer Touch

### 7.4 Booking-Status-Banner

`AWAITING_BOOKING`:

- deutlicher Banner mit Gewinnernamen
- fuer Organisator zwei klare Aktionen:
  - `Buchung bestaetigen`
  - `Buchung fehlgeschlagen`

`BOOKED`:

- bestaetigte Unterkunft als hervorgehobenes Ergebnis
- historische Poll-Daten bleiben sichtbar, aber klar sekundar

`BOOKING_FAILED` Historie:

- als `<details>`-Bereich unterhalb der aktiven Kandidaten
- visuell gedaempft, aber lesbar

---

## 8. Architekturfolgen des Redesigns

### 8.1 Bounded Context und Aggregate

Das Redesign bleibt innerhalb des Trips-SCS und respektiert die vorhandene Aggregate-Struktur:

- `AccommodationPoll` bleibt das fuehrende Aggregat fuer Entscheidung und Buchungsstatus
- `Accommodation` entsteht erst nach erfolgreichem Booking

### 8.2 Relevante Modellentscheidungen

Fuer das UX-Ziel sind folgende Modellentscheidungen nicht optional, sondern direkt
darstellungsrelevant:

- `Amenity` auf `AccommodationCandidate`
- `CandidateRoom` ohne `features`
- expliziter `CandidateStatus`
- `BookingAttempt` mit Historie

Ohne diese Modellkorrekturen waere die gewuenschte Darstellung nur mit UI-Tricks moeglich und
bliebe fachlich inkonsistent.

### 8.3 Representation Contract

Die UI sollte nicht rohe Domainstrukturen zusammensetzen muessen. Die Representation fuer die
Overview-Seite sollte deshalb mindestens liefern:

- `status`
- `activeCandidateCount`
- `totalVotes`
- `leadingCandidateName`
- `selectedCandidate`
- `failedCandidates`
- pro Kandidat:
  - `candidateId`
  - `name`
  - `url`
  - `address`
  - `description`
  - `amenities`
  - `rooms`
  - `voteCount`
  - `voteSharePercent`
  - `isLeading`
  - `isSelected`
  - `isFailed`
  - `canVote`

### 8.4 HTMX und Fragmentstrategie

Empfehlung:

- Grobe Statuswechsel bleiben PRG:
  - Poll erstellen
  - Gewinner auswaehlen
  - Buchung erfolgreich
  - Buchung fehlgeschlagen
- Feingranulare Interaktionen duerfen HTMX nutzen:
  - Stimme aendern
  - optional Kartenfragment austauschen
  - optional Importformular vorbefuellen

Grund:

- weniger Zustandskomplexitaet bei fachlich relevanten Wechseln
- gezielter UX-Gewinn bei wiederholten Kleinaktionen

---

## 9. Requirements-Sicht: UX-Redesign als umsetzbare Story-Slices

### 9.1 Must-have UX fuer v0.15.0

#### UX-1: Mobil lesbare Overview

**Als** Teilnehmer **moechte ich** auf dem Smartphone alle Unterkunftskandidaten ohne
Horizontal-Scroll vergleichen koennen, **damit** ich schnell abstimmen kann.

Acceptance Criteria:

- **Given** ich oeffne die Poll-Seite auf `<= 767px`, **When** die Kandidaten angezeigt werden,
  **Then** werden sie als vertikale Karten dargestellt.
- **Given** eine Karte ist sichtbar, **When** ich sie lese, **Then** sehe ich Name, Lage,
  Ausstattung, Zimmer, Stimmen und Aktion in stabiler Reihenfolge.
- **Given** die Poll-Seite ist mobil geoeffnet, **When** ich scrolle, **Then** gibt es keinen
  horizontalen Scroll aufgrund von Tabellen oder Formularreihen.

#### UX-2: Klarer Poll-Status

**Als** Teilnehmer **moechte ich** den Planungsstatus auf einen Blick verstehen,
**damit** ich weiss, ob noch abgestimmt, gebucht oder neu entschieden wird.

Acceptance Criteria:

- **Given** die Poll-Seite laedt, **When** der obere Seitenbereich sichtbar ist, **Then** werden
  KPI-Kacheln und Status klar lesbar angezeigt.
- **Given** der Poll ist `AWAITING_BOOKING`, **When** ich die Seite oeffne, **Then** ist ein
  gut sichtbarer Status-Banner vor den Kandidatenkarten sichtbar.
- **Given** die letzte Buchung ist fehlgeschlagen, **When** ich die Seite oeffne, **Then** wird
  dies mit naechstem Schritt kommuniziert.

#### UX-3: Ruhige, strukturierte Kandidatenkarten

**Als** Teilnehmer **moechte ich** jede Unterkunft wie ein kompaktes Listing lesen koennen,
**damit** ich eine informierte Entscheidung treffe.

Acceptance Criteria:

- **Given** ein Kandidat hat URL, Adresse und Amenities, **When** die Karte gerendert wird,
  **Then** werden diese Informationen nicht als Rohtextblock, sondern strukturiert dargestellt.
- **Given** ein Kandidat hat mehrere Zimmer, **When** die Karte gerendert wird, **Then** werden
  diese als einzelne Schlaf-/Kapazitaetsbausteine angezeigt.
- **Given** eine Adresse oder ein Ortsbezug ist vorhanden, **When** die Karte sichtbar ist,
  **Then** gibt es eine klar lesbare Kartenaktion.

#### UX-4: Mobil bedienbares Erstellformular

**Als** Organisator **moechte ich** Kandidaten auch auf dem Smartphone oder Tablet erfassen
koennen, **damit** ich unterwegs Vorschlaege schnell hinzufuegen kann.

Acceptance Criteria:

- **Given** ich bin auf Mobile, **When** ich einen Kandidatenblock bearbeite, **Then** sind alle
  Eingaben ohne Zoom bedienbar.
- **Given** ich fuege Zimmer hinzu, **When** neue Zimmerzeilen erscheinen, **Then** umbrechen sie
  in logische Feldgruppen statt seitlich auszulaufen.
- **Given** ich waehle Amenities, **When** ich durch die Liste gehe, **Then** sind Checkbox und
  Label als gemeinsame Touch-Ziele nutzbar.

### 9.2 Should-have UX

- Inline-Bestätigung nach erfolgreicher Stimmabgabe
- visuelle Kennzeichnung des fuehrenden Kandidaten in Karte und Balkenbereich
- kleine importbezogene Hilfe fuer Organisatoren: "Link einfuegen, Daten werden vorbefuellt"

---

## 10. Konkretes Screen-Verhalten

### 10.1 Mobile Overview

Empfohlener Flow:

1. Hero lesen
2. KPI-Kacheln scannen
3. Balkenvergleich ansehen
4. durch Kandidatenkarten scrollen
5. Stimme abgeben

UX-Entscheidung:

- zuerst Ergebnisorientierung, dann Detailorientierung

### 10.2 Desktop Overview

Empfohlener Flow:

1. Hero und KPI in einem Blick erfassen
2. links Ergebnisbalken, rechts Kandidatenliste oder darunter Kandidatenraster
3. Organisator-Aktionen als eigener Arbeitsbereich

UX-Entscheidung:

- Desktop darf kompakter sein, aber nicht tabellarisch-kalt wirken

### 10.3 Mobile Create

Empfohlener Flow:

1. Kandidatenblock oeffnen
2. Basisdaten ausfuellen
3. Amenities antippen
4. Zimmer in gestapelten Fieldsets pflegen
5. speichern

UX-Entscheidung:

- ein Kandidat pro visuellem Block
- keine dicht gepackten Mehrspaltenreihen

---

## 11. Design-Qualitaetsziele

### 11.1 Lesbarkeit

- Fliesstext und Meta-Text muessen bei `375px` ohne Zoom lesbar sein
- jede Karte braucht klare Abstaende zwischen Inhaltsgruppen

### 11.2 Touch und Accessibility

- interaktive Elemente mindestens `44px` Zielhoehe
- Status nicht nur ueber Farbe kommunizieren
- Links und Buttons muessen mit Tastatur erreichbar sein
- `<details>`-Bereiche muessen ohne JS nutzbar sein

### 11.3 Internationalisierung

- deutsche und englische Texte duerfen Layout nicht brechen
- KPI-Labels, Amenity-Labels und Booking-Status werden i18n-basiert ausgeliefert

### 11.4 Performance

- keine schweren Third-Party-Assets
- keine Bildpflicht fuer Kandidaten
- HTML-Struktur bleibt server-rendered und leichtgewichtig

---

## 12. Risiken und Gegenmassnahmen

### Risiko 1: Zu viele Informationen pro Karte

Gegenmassnahme:

- strikte Reihenfolge
- Beschreibung optional und gekuerzt
- fehlgeschlagene Kandidaten aus dem aktiven Bereich herausnehmen

### Risiko 2: Desktop-only Denken kehrt zurueck

Gegenmassnahme:

- Review immer zuerst bei `375px`
- erst danach Desktop verfeinern

### Risiko 3: Representation wird zu roh oder zu duenn

Gegenmassnahme:

- View-spezifische Representation aufbauen
- keine stringbasierten Template-Konstruktionen fuer komplexe Anzeige

### Risiko 4: HTMX-Fragmente kollidieren mit mobilem Layout

Gegenmassnahme:

- nur vollstaendige Karten oder klar abgegrenzte Bereiche austauschen
- keine serverseitige Mobile-Erkennung notwendig machen

---

## 13. Empfohlene Delivery-Reihenfolge aus UX-Sicht

1. Informationsarchitektur und Representation Contract festziehen
2. Candidate- und Room-Modell bereinigen
3. Mobile Create-Form reparieren
4. Overview mit KPI, Balken und Kartenstruktur bauen
5. Booking-Banner und Failure-Historie integrieren
6. Feinschliff fuer Desktop-Komfort und HTMX-Details

---

## 14. Review-Checkliste fuer Iteration 15

- Ist die gesamte Seite bei `375x812` ohne Horizontal-Scroll nutzbar?
- Sind Kandidaten auf Mobile als ruhige, gut lesbare Karten erkennbar?
- Ist der Unterschied zwischen `OPEN`, `AWAITING_BOOKING`, `BOOKED` und Fehlschlag sofort klar?
- Ist die Kandidatenbewertung ohne Roh-URLs und Freitext-Chaos moeglich?
- Fuehrt jede primaere Aktion zu einem sichtbaren Ergebnis?
- Bleibt das Trips-SCS innerhalb der bestehenden DDD- und Thymeleaf/HTMX-Grenzen?

---

## 15. Ergebnis

Das Iteration-15-Redesign soll kein kosmetisches Restyling sein, sondern ein fachlich korrektes,
mobil belastbares Entscheidungswerkzeug. Die UX gewinnt nur dann sichtbar, wenn Datenmodell,
Representation und Seitenstruktur gemeinsam angepasst werden. Genau deshalb muessen UX, Architektur
und Requirements in dieser Iteration zusammen geplant und geliefert werden.
