# ADR-0020: Trip.dateRange bleibt Pflichtfeld bei DatePoll-Integration

## Status

Ersetzt durch ADR-0021

## Context

Mit der Einfuehrung des DatePoll-Aggregats (ADR-0019) stellt sich die Frage, wie `Trip.dateRange` bei Terminabstimmung behandelt wird. Aktuell ist `dateRange` ein Pflichtfeld im Trip-Aggregat — es wird bei `Trip.plan()` gesetzt und ist als `final`-Feld deklariert. Die `DateRange` wird fuer StayPeriod-Validierung (`stayPeriod.isWithin(dateRange)`), MealPlan-Generierung (Slots pro Tag im Zeitraum), Accommodation-Logik und TripCreated-Events verwendet.

Durch den DatePoll kann der endgueltige Reisezeitraum erst nach der Abstimmung feststehen (Hot Spot HS-1 im EventStorming). Es stellt sich die Frage, ob `dateRange` weiterhin Pflichtfeld bleibt oder optional wird.

**Decision Drivers**:

1. Das Trip-Aggregat hat aktuell ein finales `dateRange`-Feld — dutzende Stellen setzen einen gueltigen Zeitraum voraus
2. StayPeriod-Validierung prueft `stayPeriod.isWithin(dateRange)` — bei `null` wuerde dies ueberall Null-Handling erfordern
3. MealPlan-Generierung und Accommodation-Logik basieren auf einem vorhandenen Zeitraum
4. DatePoll ist ein optionales Planungswerkzeug — viele Trips werden weiterhin ohne Poll erstellt
5. Bei Zeitraum-Aenderung durch DatePollConfirmed koennten bestehende StayPeriods ausserhalb des neuen Zeitraums liegen (Hot Spot HS-2)

**Betrachtete Optionen**:

- **Option A**: `dateRange` wird Optional (`null` bis DatePoll confirmed) — erfordert Breaking Change am Trip-Aggregat und Null-Handling in StayPeriod-Validierung, MealPlan-Generierung, Accommodation-Logik, TripCreated-Event und allen Controllern/Templates
- **Option B**: `dateRange` bleibt Pflichtfeld, Organizer setzt initialen "Vorschlag", DatePoll ueberschreibt bei Bestaetigung — kein Breaking Change
- **Option C**: Neuer TripStatus `DRAFT` ohne `dateRange`, erst bei Zeitraum-Festlegung Wechsel zu `PLANNING` — groesserer Umbau der Trip-Statusmaschine

## Decision

**Option B** — `Trip.dateRange` bleibt Pflichtfeld. Der Organizer setzt bei `Trip.plan()` einen initialen Zeitraum als Vorschlag. Wenn ein DatePoll erstellt und bestaetigt wird, ueberschreibt `Trip.confirmDateRange(DateRange)` den bestehenden Zeitraum.

### Neue Methode auf dem Trip-Aggregat

```java
public void confirmDateRange(final DateRange newDateRange) {
    assertStatus(TripStatus.PLANNING, "confirmDateRange");
    argumentIsNotNull(newDateRange, "newDateRange");
    this.dateRange = newDateRange;  // dateRange wird non-final
    resetStayPeriods();
}
```

### Aenderung am Trip-Aggregat

Das Feld `dateRange` wird von `final` zu non-`final` geaendert — die einzige strukturelle Aenderung am bestehenden Aggregat. Die Null-Invariante (`argumentIsNotNull(dateRange, "dateRange")` im Konstruktor) bleibt erhalten.

### StayPeriod-Zuruecksetzung

Bei `confirmDateRange()` werden alle bestehenden StayPeriods zurueckgesetzt (auf `null`), da sie moeglicherweise ausserhalb des neuen Zeitraums liegen. Teilnehmer muessen ihre Aufenthaltsdauer neu eingeben. Dies wird ueber ein Domain Event signalisiert, damit die UI die Teilnehmer informieren kann.

### Application-Service-Orchestrierung

`ConfirmDatePollService` orchestriert innerhalb des Trips-Bounded-Context:

1. `DatePoll.confirm(confirmedOptionId)` → registriert `DatePollConfirmed`
2. `Trip.confirmDateRange(confirmedOption.dateRange())` → aktualisiert Zeitraum + setzt StayPeriods zurueck
3. Beide in derselben Transaktion (kein Cross-SCS-Event noetig)

## Consequences

### Positiv

- **Kein Breaking Change**: Bestehende StayPeriod-Validierung, MealPlan-Generierung und Accommodation-Logik funktionieren unveraendert weiter
- **Kein Null-Handling**: Ein Trip hat immer einen gueltigen Zeitraum — kein defensives Null-Handling in dutzenden Stellen noetig
- **Initialer Zeitraum als Arbeitshypothese**: Der Organizer kann bereits waehrend der Abstimmung einen vorlaeufigen Plan erstellen (MealPlan, Accommodation)
- **DatePoll bleibt optional**: Trips ohne Poll funktionieren exakt wie bisher — keine Regression
- **Bestehende Tests bleiben gruen**: Keine Anpassung der vorhandenen 595+ Tests erforderlich

### Negativ

- **Semantische Unschaerfe**: Ein Trip hat ab Erstellung einen Zeitraum, obwohl dieser moeglicherweise noch zur Abstimmung steht — die UI muss dies klar kommunizieren (z.B. "Vorschlag" vs. "Bestaetigt")
- **StayPeriod-Verlust bei Zeitraumwechsel**: Teilnehmer muessen ihre Aufenthaltsdauer nach `confirmDateRange()` neu eingeben — akzeptabler Trade-off, da der Zeitraum sich grundlegend geaendert hat
- **MealPlan-Regeneration noetig**: Bei Zeitraumwechsel muss der MealPlan regeneriert werden koennen (existierender "Aktualisieren"-Mechanismus aus ADR-0015 ist wiederverwendbar)
- **`dateRange` wird non-final**: Das Feld verliert seine Immutabilitaet, was die Aggregat-Semantik leicht aufweicht — durch die enge Einschraenkung auf `confirmDateRange()` im PLANNING-Status ist dies vertretbar

## Alternatives

### Option A: dateRange wird Optional

- **Vorteile**: Semantisch korrekt — kein Zeitraum bis zur Entscheidung
- **Nachteile**: Breaking Change am Trip-Aggregat, Null-Handling in StayPeriod-Validierung (`isWithin`), MealPlan-Generierung, Accommodation-Logik, TripCreated-Event, allen Controllern und Templates. Geschaetzter Aufwand: 20+ Dateien betroffen, hohe Regressionsgefahr

### Option C: Neuer TripStatus DRAFT

- **Vorteile**: Klare Trennung zwischen "noch kein Zeitraum" und "Zeitraum gesetzt"
- **Nachteile**: Groesserer Umbau der Trip-Statusmaschine (DRAFT → PLANNING → CONFIRMED → ...), Anpassung aller Status-Checks, Flyway-Migration fuer neuen Enum-Wert, UI-Anpassung fuer DRAFT-Ansicht. Ueberengineert fuer den aktuellen Anwendungsfall

## Related

- ADR-0019: Separate Poll-Aggregate (DatePoll + AccommodationPoll)
- ADR-0008: DDD + Hexagonale Architektur
- ADR-0015: Shopping List Aggregate Design (Vorbild fuer Aggregate-Methoden-Design)
- EventStorming Iteration 14: Hot Spot HS-1 (Trip.dateRange-Aktualisierung) und HS-2 (StayPeriod-Invalidierung)
