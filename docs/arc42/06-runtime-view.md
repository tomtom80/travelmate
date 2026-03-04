# 6. Laufzeitsicht

## Szenario 1: Login-Flow (OIDC)

```
Browser        Gateway        IAM-SCS        Keycloak
  │               │              │               │
  │──GET /────────▶              │               │
  │               │──Redirect────▶               │
  │               │              │──Auth Request──▶
  │◀──────────────────────────────────Login Page──│
  │──Credentials──────────────────────────────────▶
  │               │              │◀─ID Token──────│
  │               │              │──Validate Token─▶
  │◀──Session + Redirect─────────│               │
```

1. Benutzer ruft die Anwendung auf
2. Gateway leitet an den IAM-SCS weiter
3. Spring Security erkennt fehlende Authentifizierung und leitet zu Keycloak weiter
4. Benutzer authentifiziert sich bei Keycloak (OIDC)
5. IAM-SCS validiert den ID-Token und erstellt eine Session

## Szenario 2: Trip erstellen

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST /trips──▶              │               │
  │               │──Route───────▶               │
  │               │              │──INSERT Trip───▶
  │               │              │◀──OK───────────│
  │◀──HTML (HTMX partial)───────│               │
```

1. Organisator erstellt einen neuen Trip über das Formular
2. Gateway routet den Request an den Trips-SCS
3. Trips-SCS validiert die Eingaben und persistiert den Trip
4. Thymeleaf rendert das HTML-Fragment, HTMX tauscht den DOM-Bereich aus

## Szenario 3: Rollenzuweisung und Teilnehmer-Aktivierung

```
IAM-SCS              Kafka                Trips-SCS
  │                    │                      │
  │──RoleAssignedToUser▶                      │
  │                    │──Event consumed───────▶
  │                    │                      │──Activate Participant
  │                    │                      │
```

1. Administrator weist einem Benutzer die Rolle `role/trips.organizer` oder `role/trips.participant` zu
2. IAM publiziert ein `RoleAssignedToUser`-Event auf den Kafka-Topic `role-assigned`
3. Trips-SCS konsumiert das Event und aktiviert den Benutzer als Organizer oder Participant
4. Bei `RoleUnassignedFromUser` wird der Benutzer entsprechend deaktiviert

## Szenario 4: Abrechnung und Saldo

```
Browser        Expense-SCS     PostgreSQL
  │               │               │
  │──POST Receipt─▶               │
  │               │──INSERT───────▶
  │               │               │
  │──GET Settlement▶              │
  │               │──Calculate────│
  │               │  (Weightings) │
  │◀──Settlement──│               │
```

1. Teilnehmer erfasst einen Beleg (Receipt) mit Foto und Betrag
2. Expense-SCS speichert den Beleg
3. Bei Abfrage der Abrechnung werden alle Belege eines Trips aggregiert
4. Gewichtungen (Erwachsener=1.0, Teilzeit=0.5, Kind<3=0.0) bestimmen die Aufteilung
5. Pro Familie wird ein Saldo (Settlement) berechnet

## Szenario 5: Account-Registrierung

```
Browser        Gateway        IAM-SCS        PostgreSQL     RabbitMQ
  │               │              │               │              │
  │──POST account─▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──INSERT────────▶              │
  │               │              │◀──OK──────────│              │
  │               │              │──AccountRegistered───────────▶
  │◀──Redirect to detail────────│               │              │
```

1. Benutzer fuellt das Registrierungsformular aus (Username, E-Mail, Name, Keycloak User ID)
2. Gateway routet den Request an den IAM-SCS
3. IAM-SCS prueft Username-Eindeutigkeit und persistiert den Account
4. `AccountRegistered`-Event wird nach Commit via RabbitMQ publiziert
5. Trips-SCS konsumiert das Event und legt eine TravelParty-Projektion an

## Szenario 6: Dependent hinzufuegen (HTMX)

```
Browser                     IAM-SCS        PostgreSQL     RabbitMQ
  │                            │               │              │
  │──hx-post /dependents───────▶               │              │
  │                            │──INSERT────────▶              │
  │                            │◀──OK──────────│              │
  │                            │──DependentAddedToTenant──────▶
  │◀──HTML Fragment (HTMX)─────│               │              │
```

1. Guardian klickt auf "Mitreisenden hinzufuegen" im Account-Detail
2. HTMX sendet POST via `hx-post`, Ziel ist das Dependent-Fragment
3. IAM-SCS prueft ob Guardian existiert, erstellt Dependent
4. `DependentAddedToTenant`-Event wird via RabbitMQ publiziert
5. Thymeleaf rendert das aktualisierte Fragment, HTMX tauscht den DOM-Bereich aus

## Referenz

![Event Storming](../../design/evia.team.orc.thomas-klingler%20-%20Event%20Storming.jpg)
