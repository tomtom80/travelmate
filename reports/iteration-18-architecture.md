# Iteration 18 — Architektur-Plan

**Datum**: 2026-04-26
**Target Version**: v0.18.0
**State after**: v0.17.0 (commit `cafd37f`)
**Input**: ADR-0023, Iteration-17-Plan, offene Architektur-Threads, Quality-Storming-Vokabular (INNOQ)

**Status**: PROPOSED — Architekturteil. Story-Schnitt erfolgt im Delivery-Plan.

---

## 1. Ausgangslage

Iteration 17 hat den Trip-Lebenszyklus um Edit/Delete vervollstaendigt und mit `TripDeleted` (ADR-0023) das erste reine *Cleanup-Event* ueber die SCS-Grenze etabliert. Der Eventkatalog im Shared Kernel wachst:

- **IAM** (10 Events): `AccountRegistered`, `MemberAddedToTenant`, `DependentAddedToTenant`, `DependentRemovedFromTenant`, `MemberRemovedFromTenant`, `TenantCreated`, `TenantDeleted`, `TenantRenamed`, `RoleAssignedToUser`, `RoleUnassignedFromUser`
- **Trips** (9 Events): `TripCreated`, `ParticipantJoinedTrip`, `ParticipantRemovedFromTrip`, `TripCompleted`, `TripDeleted`, `InvitationCreated`, `ExternalUserInvitedToTrip`, `StayPeriodUpdated`, `AccommodationPriceSet`

Parallel dazu sind vier strukturelle Threads aufgelaufen, die in Iteration 18 architektonisch behandelt werden sollten — bevor sie sich gegenseitig blockieren:

1. **E2E-Flakiness**: `07-expense-navigation-and-lifecycle.feature` zeigt zwei nichtdeterministische Fehlerklassen (Race auf Navigation; `ERR_NETWORK_IO_SUSPENDED`). Symptome auf Frontend-Ebene, Wurzel in fehlender Observability/Synchronisationsstrategie HTMX <-> Test-Runner.
2. **Multi-Organizer Role Management** (E-IAM-05): Trip-Aggregat hat bereits `organizerIds: List<UUID>` und `grantOrganizerRights(...)` — die *Modell*-Frage ist also entschieden, offen ist die *Governance*-Frage: Wer darf weitere Organizer ernennen, und ist Organizer eine Keycloak-Rolle (global) oder eine Trip-lokale Rolle?
3. **GlobalExceptionHandler-Duplikation**: drei nahezu identische Klassen in IAM, Trips, Expense (Trips besitzt zusaetzlich i18n via `MessageSource` und `ResponseStatusException`-Handler). Drift-Risiko bei jedem zukuenftigen Fehlertyp.
4. **Event-Versionierung & Naming-Konsistenz**: 19 Events, kein Versionsschema, keine Schema-Registry, leichte Naming-Inkonsistenzen (`AccommodationPriceSet` vs. `*Created/*Set/*Assigned`).

---

## 2. Architekturentscheidungen-Tabelle

| ADR | Status | Entscheidung | Begruendung | Abhaengigkeit |
|-----|--------|--------------|-------------|---------------|
| **ADR-0024** (NEU) | Vorgeschlagen | **Organizer-Rolle ist Trip-lokale Aggregat-Eigenschaft, nicht Keycloak-Rolle.** Keycloak-Rolle `organizer` bleibt nur Authorisierungsklammer (darf ueberhaupt Trips anlegen); operative "Organizer-eines-konkreten-Trips" wird ueber `Trip.organizerIds` verwaltet. Granting/Revoking durch bestehenden Organizer (1-Organizer-Quorum) via neuer Command `GrantOrganizerRoleCommand` / `RevokeOrganizerRoleCommand`. | Modell existiert bereits im Aggregat (`Trip.grantOrganizerRights`). Promotion zur Keycloak-Rolle wuerde Tenant- und Trip-Granularitaet vermischen und Cross-Tenant-Lecks oeffnen (Keycloak-Rollen sind globale Fakten, Trip-Organizer-Status ist trip-skopiert). Ausserdem skaliert Trip-lokal mit dem bestehenden Choreographie-Pattern (`OrganizerRoleGranted`/`Revoked` Domain-Events). | Voraussetzung fuer S18-Multi-Organizer-Stories (E-IAM-05). |
| **ADR-0025** (NEU) | Vorgeschlagen | **Event-Versionierung via Klassen-Suffix (`V1`, `V2`) bei *brechenden* Schemaaenderungen; additive Felder ohne Versionswechsel.** Routing-Key bleibt unveraendert; Consumer deklarieren Pflichtfelder ueber Konstruktor-Pattern. Schema-Registry wird *nicht* eingefuehrt. Naming-Konvention: Event-Records enden auf Vergangenheitsform (`*Created`, `*Updated`, `*Deleted`, `*Granted`, `*Revoked`); `*Set` wird in Folgeschritten zu `*Updated` migriert (Soft-Deprecation). | RabbitMQ-Topic-Exchange erlaubt parallel `vN`/`vN+1`-Routing-Keys, aber das ist Overengineering fuer den heutigen Eventvolumen. Klassen-Suffix ist *explizit*, vermeidet stillschweigende Kompatibilitaetsbruchstellen, und ein lightweight Konzept ohne externe Infrastruktur passt zur Self-Contained-Systems-Philosophie. | Voraussetzung fuer kuenftige Event-Erweiterungen (z.B. `OrganizerRoleGranted`, Kuechendienst-Events). |
| **ADR-0026** (NEU) | Vorgeschlagen | **GlobalExceptionHandler nach `travelmate-common` extrahieren** — als abstrakte Basisklasse `AbstractGlobalExceptionHandler` mit Default-Handlern; SCS-spezifische Subklasse darf um spezielle `@ExceptionHandler` erweitern (z.B. `ResponseStatusException` in Trips). i18n `MessageSource`-Resolver wird in der Basisklasse Pflicht, alle drei SCS pflegen `messages.properties`. | Drei nahezu identische Klassen ohne strukturierten Vererbungspfad sind ein klarer DRY-Verstoss; jeder neue Domain-Exception-Typ (z.B. `ConcurrencyConflictException` in einem zukuenftigen Optimistic-Locking-Story) verdoppelt sich heute dreifach. Common ist heute schon die richtige Heimat fuer Cross-Cutting-Concerns (Assertion, AggregateRoot, DomainEvent). | Senkt Wartungskosten fuer alle nachfolgenden Iterationen. |
| **ADR-0023** | Akzeptiert | Trip-Loeschen mit kaskadierender Event-Propagation. | (Iteration 17, kein Update noetig.) | — |

> Hinweis: Der Architekturteil sieht *keine* ADR fuer E2E-Stabilisierung vor. Die HTMX-Synchronisationsverhaltensweisen sind bereits in ADR-0013 (HTMX Feedback) dokumentiert; Iteration 18 fuehrt eine *Test-Strategie-Erweiterung* durch, nicht eine Architektur-Entscheidung.

---

## 3. Quality-Attribute-Analyse (Quality Storming)

ISO-25010 Hauptkategorien, gemappt auf vier Iter-18-Kandidaten-Threads. Bewertung: **+++** stark, **++** spuerbar, **+** moderat, **0** neutral, **-** moeglicher Trade-off.

| Quality Attribute (ISO 25010) | E2E-Flakiness beheben | Multi-Organizer (ADR-0024) | Event-Versionierung (ADR-0025) | ExceptionHandler-Dedup (ADR-0026) |
|--------------------------------|----------------------|----------------------------|--------------------------------|------------------------------------|
| **Reliability — Maturity**      | +++ (deterministische Pipeline) | + (zusaetzliche Edge Cases im Trip) | ++ (Compatibility-Garantien) | + (gleichformiges Errorverhalten) |
| **Reliability — Recoverability** | + (klarere Fehlersignale) | 0 | +++ (Konsumenten-Lag wird wartbar) | ++ (uniforme HTMX-Toast-Semantik) |
| **Maintainability — Modularity**| 0 | ++ (Trip behaelt Owner-Schaft der Rolle) | +++ (Schema-Evolution explizit) | +++ (Single Source of Truth) |
| **Maintainability — Reusability**| 0 | + (Pattern fuer Aggregat-lokale Rollen) | ++ (Pattern fuer alle Eventtypen) | +++ (Common-Erweiterung) |
| **Maintainability — Testability**| +++ (stabile CI = Testpyramid intakt) | + (klare Aggregat-Tests) | + (Versionsbruch-Detektion via ArchUnit moeglich) | ++ (ein einziger Handler-Test pro Verhalten) |
| **Security — Confidentiality**  | 0 | +++ (Trip-Skopierung verhindert Cross-Tenant-Promotions) | 0 | 0 |
| **Security — Integrity**        | + (Race-Detection) | ++ (Authorisierungs-Guards im Aggregat) | + (Schema-Drift wird sichtbar) | + (kein vergessener Errorpfad) |
| **Performance — Time Behaviour**| + (kein Test-Retry-Overhead) | 0 | 0 | 0 |
| **Functional Suitability — Correctness** | ++ (Tests vertrauenswuerdig) | +++ (Multi-Organizer ist Backlog-Pflicht) | ++ (Konsumenten zerbrechen nicht still) | + (deterministische Statuscodes) |
| **Compatibility — Co-existence**| 0 | 0 | +++ (Hauptmotivation) | 0 |

### Quality-Storming-Szenarien (Auszug)

```markdown
### QS-018-A: E2E-Pipeline-Stabilitaet
Quality Attribute: Reliability — Maturity (Testbarkeit der CI-Pipeline)
Stimulus: Vollstaendiger E2E-Lauf gegen Docker-Compose-Infrastruktur
Environment: GitHub Actions / lokale Maschine, normaler Build
Response: Pipeline gruen ohne Retry, alle Szenarien deterministisch
Response Measure: 0 transiente Fehler in 20 aufeinanderfolgenden Laeufen
Metric: Anzahl Playwright-Retries pro Iteration (Ziel: 0)

### QS-018-B: Multi-Organizer-Authorisierungsgrenze
Quality Attribute: Security — Confidentiality + Functional Correctness
Stimulus: Organizer A versucht, Account aus Tenant B als Trip-Organizer zu ernennen
Environment: Produktion, normale Last
Response: Aggregat-Guard wirft BusinessRuleViolationException; HTTP 422
Response Measure: 100% Ablehnung; null Cross-Tenant-Promotions im Audit-Log

### QS-018-C: Event-Schema-Evolution
Quality Attribute: Compatibility — Co-existence
Stimulus: Producer publiziert TripCreatedV2 mit neuem Pflichtfeld
Environment: Rolling Deploy, alter Consumer noch live
Response: Alter Consumer ignoriert V2-Routing-Key; neuer Consumer verarbeitet beide
Response Measure: 0 Nachrichten in DLQ wegen Deserialisierungsfehler

### QS-018-D: Errorpfad-Konsistenz
Quality Attribute: Maintainability — Modularity + Usability — User Error Protection
Stimulus: Identische `BusinessRuleViolationException` in IAM, Trips und Expense
Environment: HTMX- und Klassik-Request
Response: Identischer Statuscode (422), identischer Toast-Mechanismus
Response Measure: 100% Verhaltensgleichheit ueber alle drei SCS (Contract-Test)
```

### Observable Metrics

| Metric ID | Name | Quelle | Ziel | Werkzeug |
|-----------|------|--------|------|----------|
| M-018-01 | Playwright-Retry-Rate | CI-Reports | 0/Lauf | GitHub Actions Artefakte |
| M-018-02 | Cross-Tenant-Promotion-Versuche | Audit-Log | 0 erfolgreich | Anwendungsloggen |
| M-018-03 | Events in DLQ pro Tag | RabbitMQ Management API | < 1 | Prometheus-Scrape |
| M-018-04 | Event-Schema-Drift-Verstoesse | ArchUnit-Tests | 0 | Maven-Build |
| M-018-05 | Duplikatzeilen in *ExceptionHandler* | jscpd / SonarQube | < 30 | Build-Metric |

---

## 4. Risiken & Mitigationen

| ID | Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|----|--------|--------------------|--------|------------|
| R-018-01 | E2E-Race-Conditions sind nicht reproduzierbar lokal | Wahrscheinlich | Hoch | Stress-Run mit `--repeat-each=10`; HTMX-Settle-Hooks instrumentieren; Playwright `waitForLoadState('networkidle')` *und* HTMX-spezifisches `htmx:afterSettle`-Event abwarten. |
| R-018-02 | Multi-Organizer-Promotion erlaubt Privilege Escalation, wenn Guard fehlt | Unwahrscheinlich | Kritisch | Aggregat-Tests fuer alle Guard-Pfade (Self-Demotion, Last-Organizer-Schutz, Cross-Tenant-ID); ArchUnit-Regel: `Trip.grantOrganizerRights` nur via Application-Service erreichbar. |
| R-018-03 | Event-Versionierung wird ignoriert, weil sie *jetzt* nicht weh tut | Wahrscheinlich | Mittel | ADR-0025 verbindlich machen; ArchUnit-Test prueft Naming-Konvention (`*Created/*Updated/*Deleted/*Granted/*Revoked`) sowie Pflicht von `tenantId` und `occurredOn`. |
| R-018-04 | ExceptionHandler-Refactor bricht HTMX-Toast-Verhalten, das im Frontend stillschweigend erwartet wird | Mittel | Mittel | Vor Refactoring: BDD-Szenarios pro Errortyp pro SCS. Kontrakt: `HX-Trigger`-Header und Statuscodes. |
| R-018-05 | Last-Organizer-Demotion macht Trip "unbesitzbar" | Unwahrscheinlich | Hoch | Domain-Invariante: `organizerIds.size() >= 1` zu jeder Zeit; `revokeOrganizerRole` mit Guard. |
| R-018-06 | Common-Modul wird durch ExceptionHandler-Extraktion zu schwer (Spring-Abhaengigkeit) | Mittel | Mittel | Common bleibt heute Plain JAR. ABwaegen: entweder `travelmate-common-web` Submodul mit Spring-Web-Abhaengigkeit, oder Acceptance dass die Extraktion *nicht* in Common, sondern in einen neuen `travelmate-web-commons` zieht. ADR-0026 muss diesen Punkt explizit entscheiden. |
| R-018-07 | E2E-Stabilisierung wird "Catch-all" und blockiert Story-Lieferungen | Mittel | Mittel | Time-Boxing: 2 Tage Diagnose, danach Eskalation oder akzeptiertes Restrisiko mit dokumentierten Retry-Limits. |

---

## 5. Empfehlung Architektur-Fokus

**Empfehlung: Iteration 18 dominiert E2E-Pipeline-Stabilitaet (Thread 1) als *primaeren* Architektur-Fokus, gekoppelt mit ADR-0024 (Multi-Organizer) als *funktionalen* Fokus.**

### Begruendung

Aus Sustainability-Sicht (Quality-Value-Chain-Evolution nach INNOQ) ist die *Testbarkeit* heute der schwaechste Knoten in der Wertschoepfungskette: jede zukuenftige Iteration wird durch flaky E2E-Laeufe verlangsamt, und das Vertrauen in das automatisierte Sicherheitsnetz erodiert messbar (Memory-Eintrag `feedback_always_run_e2e` dokumentiert bereits ein Muster: "wenn E2E geskippt wurde, traten Bugs auf"). Eine instabile Pipeline ist ein **Multiplikator** auf alle anderen Quality-Attribute — sie verschleiert Reliability-Probleme, verhindert Performance-Regressions-Detektion und macht Refactorings (insbesondere ADR-0026!) gefaehrlich.

ADR-0024 (Multi-Organizer) wird als *Sekundaerfokus* mitgezogen, weil:
- die Modell-Frage bereits durch das Aggregat beantwortet ist (`organizerIds: List<UUID>`),
- E-IAM-05 ein Backlog-Block fuer mehrere weitere Stories ist (Trip-Uebergabe, Co-Organizer-Einladungen),
- die Architektur-Entscheidung *klein* ist und ohne Infrastrukturaenderungen auskommt.

ADR-0025 (Event-Versionierung) und ADR-0026 (ExceptionHandler-Dedup) werden **bewusst nicht** zum Hauptfokus erhoben:
- ADR-0025 ist *vorausschauende* Hygiene; das Schmerzlevel ist heute niedrig (alle Consumer kontrolliert die gleiche Codebasis), und ein verfruehter Versionierungsapparat erzeugt Overhead ohne kurzfristigen Nutzen. ADR drafted, Implementierung folgt iterativ ab dem ersten *brechenden* Schemawechsel.
- ADR-0026 ist klassisches Refactoring; ohne stabile E2E-Pipeline waere es ein Risiko-Refactor. Wir verschieben es auf Iteration 19, *nachdem* die Pipeline geheilt ist.

### Konkrete Iter-18-Architektur-Liefergegenstaende

1. **HTMX-Test-Synchronisationsstrategie dokumentieren** in `docs/arc42/06-runtime-view.md` (neuer Abschnitt "HTMX/Playwright-Sync") und Helper-Methode in `travelmate-e2e`-Modul.
2. **ADR-0024** schreiben und akzeptieren — bevor S18-Multi-Organizer-Stories starten.
3. **ADR-0025** als *Vorgeschlagen* einchecken; ArchUnit-Test fuer Event-Naming-Konvention und `tenantId+occurredOn`-Pflicht im *Trips*-Modul (als Pilot, danach in IAM/Expense uebernehmen).
4. **ADR-0026 drafted** im `Vorgeschlagen`-Status, Implementierung deferred auf Iteration 19.
5. **Quality-Storming-Output** nach `docs/arc42/10-quality-requirements.md` einpflegen (QS-018-A bis QS-018-D + Metriktabelle).

---

## 6. ADR-Stubs (zu verfassen in Iter 18)

### ADR-0024 — Organizer-Rolle als Trip-lokale Aggregat-Eigenschaft

```markdown
# ADR-0024: Organizer-Rolle als Trip-lokale Aggregat-Eigenschaft

## Status
Vorgeschlagen

## Kontext
Trip-Aggregat besitzt bereits `organizerIds: List<UUID>` und `grantOrganizerRights(UUID)`.
Backlog-Item E-IAM-05 (Multi-Organizer Role Management) verlangt eine entscheidung,
ob die Organizer-Rolle pro Trip (Aggregat-Eigenschaft) oder global pro Account
(Keycloak-Rolle / IAM-Aggregat) verwaltet wird. Heute existiert die Keycloak-Rolle
`organizer` als Authorisierungsklammer fuer "darf ueberhaupt Trips anlegen".

## Entscheidung
Die operative Organizer-Rolle eines konkreten Trips bleibt eine Trip-lokale
Aggregat-Eigenschaft. Die Keycloak-Rolle `organizer` bleibt ausschliesslich
Berechtigungsklammer fuer das Anlegen von Trips. Promotion/Demotion innerhalb
eines Trips erfolgt via neue Application-Service-Methoden:
- `TripService.grantOrganizerRole(TripId, UUID actor, UUID promotedAccount)`
- `TripService.revokeOrganizerRole(TripId, UUID actor, UUID demotedAccount)`
mit Aggregat-Invarianten:
- `organizerIds.size() >= 1` jederzeit
- Nur bestehender Organizer darf promoten/demoten (Self-Demotion erlaubt,
  solange ein anderer Organizer existiert)
- promotedAccount muss Participant des Trips sein

## Konsequenzen
### Positiv
- Trip-Aggregat behaelt Owner-Schaft seiner Authorisierungsregeln
- Cross-Tenant-Promotions unmoeglich (Participant-Constraint impliziert Tenant)
- Konsistent mit ADR-0008 (DDD/Hexagonal) und ADR-0001 (SCS)
### Negativ
- Keine Wiederverwendung des Organizer-Status ueber Trips hinweg
  (ein Account muss pro Trip einzeln zum Organizer ernannt werden)
- Zwei "Organizer"-Begriffe im System (Keycloak-Rolle + Trip-Property),
  potenziell verwirrend - muss in `12-glossary.md` gepflegt werden

## Alternativen
### A: Keycloak-Rolle `trip-organizer-{tripId}` pro Trip
- Vorteile: Single Source of Truth in Keycloak
- Nachteile: Rollen-Explosion in Keycloak; ueberschreitet das, wofuer Keycloak gebaut ist;
  Cross-Tenant-Lecks moeglich

### B: Eigenes Aggregat `TripMembership` in IAM
- Vorteile: explizite IAM-Verantwortung
- Nachteile: doppelte Datenhaltung Trips <-> IAM; Cross-SCS-Synchronisation noetig

## Referenzen
- ADR-0001 (SCS), ADR-0008 (DDD/Hexagonal), ADR-0011 (Ubiquitous Language)
- Backlog: E-IAM-05
```

### ADR-0025 — Event-Versionierung und Naming-Konvention

```markdown
# ADR-0025: Event-Versionierung und Naming-Konvention

## Status
Vorgeschlagen

## Kontext
travelmate-common haelt aktuell 19 Events (10 IAM, 9 Trips). Es existiert keine
Versionierungsstrategie. Bei einem brechenden Schemawechsel (z.B. neues Pflichtfeld)
wuerden Consumer mit unverstaendlichen Deserialisierungsfehlern in der DLQ enden.
Naming ist leicht inkonsistent (`AccommodationPriceSet` statt `*Updated`).

## Entscheidung
1. Additive Aenderungen (neues optionales Feld) ohne Versionswechsel.
2. Brechende Aenderungen via Klassen-Suffix `V2` und parallelem Routing-Key
   (`trips.trip-created.v2`); Producer publiziert beide Versionen waehrend
   Migration; Consumer migrieren in eigener Geschwindigkeit.
3. Naming-Konvention: Event-Records enden ausschliesslich auf `*Created`, `*Updated`,
   `*Deleted`, `*Granted`, `*Revoked`, `*Joined`, `*Removed`, `*Completed`, `*Cancelled`.
   `*Set` ist deprecated; `AccommodationPriceSet` wird in Folgeschritten zu
   `AccommodationPriceUpdated` umbenannt (mit Versionsuebergangsphase).
4. Pflichtfelder fuer alle Events: `tenantId: UUID`, `occurredOn: LocalDate`.
5. ArchUnit-Tests im common-Modul setzen Naming und Pflichtfelder durch.

## Konsequenzen
### Positiv
- Schema-Evolution explizit dokumentiert
- Keine externe Schema-Registry noetig (passt zu SCS)
- Naming-Drift wird im Build sichtbar
### Negativ
- Migration bestehender Events (`AccommodationPriceSet`) ist Folgeaufwand
- Zwei parallele Eventversionen waehrend Migration erhoehen Komplexitaet kurzzeitig

## Alternativen
- Schema-Registry (Apicurio, Confluent): Overkill fuer aktuelle Groesse
- Keine Versionierung: implizite Annahme dass alle Consumer immer gemeinsam migriert werden
  - heute richtig, langfristig unsicher

## Referenzen
- ADR-0006 (RabbitMQ als Messaging-Backbone)
- https://www.enterpriseintegrationpatterns.com/patterns/messaging/MessageVersioning.html
```

### ADR-0026 — Zentralisierter GlobalExceptionHandler

```markdown
# ADR-0026: Zentralisierter GlobalExceptionHandler

## Status
Vorgeschlagen — Implementierung deferred auf Iteration 19

## Kontext
Drei nahezu identische `GlobalExceptionHandler`-Klassen in IAM, Trips, Expense.
Jeder neue Domain-Exception-Typ verdreifacht sich. Trips besitzt zusaetzlich
i18n-`MessageSource`-Resolver und `ResponseStatusException`-Handler.

## Entscheidung
Extraktion einer abstrakten Basisklasse `AbstractGlobalExceptionHandler`,
gehostet in einem neuen Modul `travelmate-web-commons` (nicht in `travelmate-common`,
da letzteres Plain-JAR ohne Spring-Web-Abhaengigkeit bleibt).
SCS-spezifische Subklassen koennen weitere `@ExceptionHandler` ergaenzen.
i18n-Aufloesung wird Default-Verhalten (alle SCS pflegen `messages.properties`).

## Konsequenzen
### Positiv
- Single Source of Truth fuer HTMX-Toast-Mechanismus
- Neue Exception-Typen werden einmal implementiert
- Kontrakt-Tests im Common-Modul moeglich
### Negativ
- Neues Modul `travelmate-web-commons` erhoeht POM-Komplexitaet
- Migration erfordert sorgfaeltige BDD-Abdeckung pro SCS, um HTMX-Verhalten nicht zu brechen

## Alternativen
- ExceptionHandler in `travelmate-common`: bricht "common ist Plain JAR"-Invariante
- Code-Generierung: Overengineering
- Status quo: akzeptierter DRY-Verstoss, waechst mit jedem Exception-Typ

## Referenzen
- ADR-0013 (HTMX Feedback und Error Handling)
```

---

## 7. Verifikationsstrategie

- **Konformanzpruefung**: ArchUnit erweitern um `events_must_have_tenantid_and_occurredon` und `events_naming_convention` (Trips als Pilot).
- **E2E-Stabilisierungs-Akzeptanzkriterium**: 20 aufeinanderfolgende `./mvnw -Pe2e verify`-Laeufe ohne Retries und ohne `Navigation interrupted`/`ERR_NETWORK_IO_SUSPENDED` in den Reports.
- **ADR-0024-Akzeptanzkriterium**: BDD-Feature `multi-organizer.feature` mit Self-Demotion-Schutz, Cross-Tenant-Schutz, Last-Organizer-Schutz.
- **Quality-Metriken**: M-018-01 bis M-018-05 in `docs/arc42/10-quality-requirements.md` als neue Tabelle.

---

## 8. Out-of-Scope

- ADR-0026 Implementierung (Iteration 19)
- Migration `AccommodationPriceSet` -> `AccommodationPriceUpdated` (Iteration 19+)
- Schema-Registry-Einfuehrung (nicht geplant)
- Kuechendienst-Fairness-Analytik — siehe Delivery-Plan; **architektonisch entschieden**: bleibt zunaechst In-Aggregate-Query (kein separater Read-Model), Re-Bewertung wenn Performance-Symptome auftreten oder Cross-Trip-Analytik gefordert wird.
