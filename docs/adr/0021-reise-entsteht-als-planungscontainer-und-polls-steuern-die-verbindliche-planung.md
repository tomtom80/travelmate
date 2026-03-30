# ADR-0021: Reise entsteht als Planungscontainer und Polls steuern die verbindliche Planung

## Status

Vorgeschlagen

## Context

Die bisherige Planung fuer kollaborative Reiseentscheidungen geht von mehreren widerspruechlichen
Annahmen aus:

1. Eine Reise verlangt schon bei `Trip.plan(...)` einen finalen `DateRange`.
2. Der endgueltige Reisezeitraum soll erst durch eine Terminabstimmung festgelegt werden.
3. Eine Unterkunft kann weiterhin direkt gepflegt werden, obwohl parallel eine demokratische
   Unterkunftsabstimmung existiert.

Damit entstehen fuer Fachlichkeit und UI mehrere konkurrierende Wahrheiten. Die Reise wird schon
wie eine terminierte Reise behandelt, obwohl die Gruppe wesentliche Entscheidungen erst in der
Planungsphase treffen soll. Gleichzeitig unterlaeuft direkte Unterkunftspflege den Anspruch, dass
die Gruppe die Unterkunft gemeinsam auswaehlt.

Die Diskussion aus Architektur-, UX- und Requirements-Sicht fuehrt zu denselben Konsequenzen:

- Eine neu angelegte Reise ist zunaechst ein Planungscontainer.
- Der verbindliche Reisezeitraum wird erst nach bestaetigter Terminabstimmung festgelegt.
- Die verbindliche Unterkunftsauswahl wird im demokratischen Standardfall erst nach bestaetigter
  Unterkunftsabstimmung uebernommen.
- Das Planning-Tool ist der kanonische Ort fuer diese Entscheidungen.

**Decision Drivers**:

1. Das System darf keine widerspruechlichen Aussagen ueber den Reisezeitraum machen.
2. Die UI muss einen klaren, nachvollziehbaren Planungsworkflow abbilden.
3. Demokratische Abstimmungen duerfen nicht durch parallele Direktpfade unterlaufen werden.
4. Die Reise-Lifecycle-Zustaende sollen fachlich nachvollziehbar und technisch pruefbar sein.
5. Bestehende Integrationen wie `TripCreated` und Downstream-Verbraucher muessen kontrolliert
   migrierbar bleiben.

## Decision

### 1. Reiseanlage erzeugt einen Planungscontainer

Beim Anlegen einer neuen Reise werden nur planungsrelevante Stammdaten wie Name und Beschreibung
erfasst. Ein finaler Reisezeitraum gehoert nicht mehr zum Pflichtumfang der Reiseanlage.

### 2. Die Terminabstimmung ist die fachliche Quelle fuer den verbindlichen Reisezeitraum

Eine Reise darf in fruehen Planungsphasen ohne finalen Reisezeitraum existieren. Erst die
bestaetigte Terminabstimmung legt den verbindlichen Reisezeitraum fest.

Das ersetzt die bisherige Annahme aus ADR-0020, dass ein initialer Zeitraum als "Vorschlag" direkt
auf dem Trip-Aggregat gespeichert wird.

### 3. Die Unterkunftsabstimmung ist der Standardpfad zur verbindlichen Unterkunftsauswahl

Solange sich eine Reise in der kollaborativen Planungsphase befindet, wird keine verbindliche
Unterkunft direkt angelegt oder bearbeitet. Eine konkrete Unterkunft wird im demokratischen
Standardfall erst dann als verbindliches Planungsergebnis uebernommen, wenn die
Unterkunftsabstimmung bestaetigt wurde.

Falls spaeter ein expliziter alternativer Planungsmodus wie `ORGANIZER_DECIDES` benoetigt wird, ist
dies als eigenstaendige Fachentscheidung mit eigener ADR zu modellieren. Ein stiller Bypass ist
nicht zulaessig.

### 4. Der Trip-Lifecycle wird in Planungsreife unterteilt

Die Reise-Lifecycle-Logik wird fachlich in mindestens folgende Schritte gegliedert:

1. `PLANNING` oder ein vergleichbarer frueher Planungszustand ohne verpflichtenden finalen Zeitraum
2. `READY` fuer fachlich vollstaendig geplante Reisen mit bestaetigtem Reisezeitraum und
   bestaetigter Unterkunftsentscheidung
3. `CONFIRMED` als explizite organisatorische Freigabe
4. `IN_PROGRESS`, `COMPLETED`, `CANCELLED` unveraendert

Ob fuer den fruehen Planungszustand ein neuer Enum-Wert wie `DRAFT` eingefuehrt wird oder ob
`PLANNING` semantisch erweitert wird, ist eine Implementierungsentscheidung. Fachlich verbindlich
ist: Eine Reise darf vor `READY` ohne finalen Zeitraum existieren.

### 5. Das Planning-Tool wird zum kanonischen Entscheidungseinstieg

Die Planungsansicht auf der Reisedetailseite wird als Workflow fuer gemeinsame Entscheidungen
ausgerichtet. Titel, Untertitel und CTA-Texte muessen sowohl Reisezeitraum als auch Unterkunft
widerspruchsfrei benennen. Das Planning-Tool ist kein generischer Link, sondern der Einstieg in die
gemeinsame Abschlussplanung.

## Consequences

### Positiv

- Die Fachlogik wird konsistent: Der Reisezeitraum ist erst dann verbindlich, wenn er gemeinsam
  oder explizit entschieden wurde.
- Die UI kommuniziert einen klaren Ablauf: Reise anlegen, Teilnehmer einladen, Zeitraum abstimmen,
  Unterkunft abstimmen, Reise bestaetigen.
- Die Unterkunftsabstimmung erhaelt echten fachlichen Wert, weil sie nicht mehr parallel von
  direkter Unterkunftspflege entwertet wird.
- BDD- und E2E-Tests koennen den kollaborativen Planungsprozess durchgaengig abbilden.

### Negativ

- `Trip.dateRange` kann nicht mehr an allen Stellen blind als vorhanden angenommen werden.
- Vorhandene Integrationen und Events wie `TripCreated` muessen angepasst oder durch spaetere
  Bestaetigungs-Events ergaenzt werden.
- Der Lifecycle wird expliziter und damit in Implementierung, Migration und UI etwas aufwendiger.

### Technische Folgen

- Das Trip-Aggregat muss einen Zustand ohne finalen Reisezeitraum zulaassen oder ueber einen
  vorgelagerten Lifecycle-Zustand modellieren.
- `TripCreated` darf keine fachlich finalen Reisedaten mehr implizieren, solange diese noch nicht
  bestimmt wurden.
- Ein bestaetigter Reisezeitraum sollte ueber ein eigenes Domain Event wie
  `TripDateRangeConfirmed` kommuniziert werden.
- Direkte Unterkunftserfassung ist vor bestaetigter Unterkunftsentscheidung zu sperren oder in
  reine unverbindliche Vorschlaege umzudeuten.
- Planning-, Poll- und Trip-Detail-Views muessen dieselbe Prozesslogik und dieselben Preconditions
  verwenden.

## Alternatives

### Option A: Bisheriges Modell aus ADR-0020 beibehalten

- **Vorteile**: Weniger Umbau im bestehenden Trip-Aggregat
- **Nachteile**: Bleibender fachlicher Widerspruch zwischen Reiseanlage und Terminabstimmung; die UI
  muss einen vermeintlich finalen Zeitraum als "nur Vorschlag" relativieren

### Option B: Direkte Unterkunftspflege trotz Poll beibehalten

- **Vorteile**: Weniger Einschraenkung fuer Organisatoren
- **Nachteile**: Unterlaeuft die demokratische Unterkunftsabstimmung und fuehrt zu zwei
  konkurrierenden Entscheidungswegen

### Option C: Vollstaendig neuen Lifecycle mit vielen neuen Statuswerten einfuehren

- **Vorteile**: Hohe semantische Klarheit
- **Nachteile**: Hoeherer Implementierungs- und Migrationsaufwand; fuer die erste Umsetzung reicht
  ein minimaler, aber klar definierter Zwischenschritt bis `READY`

## Related

- ADR-0020: Trip.dateRange bleibt Pflichtfeld bei DatePoll-Integration
- ADR-0019: Separate Poll-Aggregate (DatePoll + AccommodationPoll)
- ADR-0011: Ubiquitous Language and Domain Terminology
- EventStorming Iteration 14: Hot Spots zur Termin- und Unterkunftsentscheidung
