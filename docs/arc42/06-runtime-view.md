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

## Szenario 3: Self-Service Sign-up (S3-A02)

```
Browser        Gateway        IAM-SCS        Keycloak       PostgreSQL     RabbitMQ
  │               │              │               │              │              │
  │──GET /signup──▶              │               │              │              │
  │               │──Route───────▶               │              │              │
  │◀──Sign-up Form──────────────│               │              │              │
  │──POST /signup─▶              │               │              │              │
  │               │──Route───────▶               │              │              │
  │               │              │──Tenant.create─────────────▶│              │
  │               │              │──createUser───▶               │              │
  │               │              │◀──keycloakUserId──│          │              │
  │               │              │──Account.register──────────▶│              │
  │               │              │──TenantCreated + AccountRegistered────────▶│
  │◀──Redirect to Login─────────│               │              │              │
```

1. Benutzer oeffnet die oeffentliche Sign-up-Seite (kein Login erforderlich)
2. Formular: Reisepartei-Name, Vorname, Nachname, E-Mail, Passwort
3. SignUpService orchestriert atomar: Tenant erstellen, Keycloak-User anlegen, Account registrieren
4. Events (TenantCreated, AccountRegistered) werden via RabbitMQ publiziert
5. Trips-SCS konsumiert die Events und legt eine TravelParty-Projektion an
6. Redirect zum Gateway Login (OIDC Flow startet automatisch)

## Szenario 3b: Teilnehmer einladen und annehmen (S3-B04)

```
Browser        Gateway        Trips-SCS      PostgreSQL     RabbitMQ
  │               │              │               │              │
  │──POST invite──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──Invitation.create──────────▶│
  │◀──HTML Fragment (HTMX)──────│               │              │
  │               │              │               │              │
  │──POST accept──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──invitation.accept()         │
  │               │              │──trip.addParticipant()       │
  │               │              │──save both────▶              │
  │               │              │──ParticipantJoinedTrip──────▶│
  │◀──HTML Fragment (HTMX)──────│               │              │
```

1. Organisator laedt ein Mitglied der Reisepartei zu einem Trip ein
2. Eingeladener sieht die Einladung mit Annehmen/Ablehnen-Buttons
3. Bei Annahme: Invitation wird ACCEPTED, Participant wird zum Trip hinzugefuegt
4. ParticipantJoinedTrip-Event wird via RabbitMQ publiziert

## Szenario 3c: Trip-Einladungs-E-Mail (Iteration 4)

```
Browser        Gateway        Trips-SCS      PostgreSQL     SMTP (Mailpit)
  │               │              │               │              │
  │──POST invite──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──Invitation.create──────────▶│
  │               │              │──InvitationCreated (enriched)│
  │               │              │──────────────────────────────▶│ (after commit)
  │◀──HTML Fragment (HTMX)──────│               │     E-Mail──▶│
```

1. Organisator laedt ein Mitglied zu einem Trip ein
2. `InvitationService` enrichiert das `InvitationCreated`-Event mit Trip-Name, Zeitraum, Einlader-Name aus Trip- und TravelParty-Aggregaten
3. Nach Transaction-Commit sendet `InvitationEmailListener` eine HTML-E-Mail via Spring Mail
4. E-Mail enthaelt Trip-Details und einen Link zur Trip-Seite

## Szenario 3d: Externe Einladung per E-Mail (Iteration 4)

```
Browser     Trips-SCS      RabbitMQ       IAM-SCS        Keycloak       SMTP
  │             │              │              │               │            │
  │──POST ──────▶              │              │               │            │
  │  external   │              │              │               │            │
  │             │──Invitation.inviteExternal()│               │            │
  │             │  [AWAITING_REGISTRATION]    │               │            │
  │             │──InvitationCreated──────────│──────────────────────────▶│ (E-Mail)
  │             │──ExternalUserInvitedToTrip──▶              │            │
  │◀──HTML──────│              │              │               │            │
  │             │              │              │               │            │
  │             │              │──consume─────▶               │            │
  │             │              │              │──createUser───▶            │
  │             │              │              │──Account.register()        │
  │             │              │              │──AccountRegistered────────▶│
  │             │              │              │               │            │
  │             │◀─consume─────│              │               │            │
  │             │──TravelParty.addMember()    │               │            │
  │             │──invitation.linkToMember()  │               │            │
  │             │──trip.addParticipant()      │               │            │
  │             │  [Auto-Accept → ACCEPTED]   │               │            │
```

1. Organisator gibt E-Mail, Name, Geburtsdatum ein und laedt eine neue Person ein
2. Trips erstellt Invitation im Status AWAITING_REGISTRATION
3. `InvitationCreated` loest E-Mail-Versand aus (Einladung mit Registrierungshinweis)
4. `ExternalUserInvitedToTrip` wird via RabbitMQ an IAM publiziert
5. IAM `ExternalInvitationConsumer` erstellt Keycloak-User und Account, publiziert `AccountRegistered`
6. Trips konsumiert `AccountRegistered`, aktualisiert TravelParty, findet wartende Einladung per E-Mail
7. Auto-Accept: Invitation → ACCEPTED, Participant wird zum Trip hinzugefuegt

## Szenario 4: Expense-Erstellung via Event-Choreografie (Iteration 5)

```
Trips-SCS      RabbitMQ       Expense-SCS      PostgreSQL
  │               │               │               │
  │──TripCreated──▶               │               │
  │               │──consume──────▶               │
  │               │               │──TripProjection.create()
  │               │               │──save──────────▶
  │               │               │               │
  │──ParticipantJoinedTrip──────▶│               │
  │               │──consume──────▶               │
  │               │               │──projection.addParticipant()
  │               │               │──save──────────▶
  │               │               │               │
  │──TripCompleted──────────────▶│               │
  │               │──consume──────▶               │
  │               │               │──Expense.create(weightings=1.0)
  │               │               │──save──────────▶
  │               │               │──ExpenseCreated (Event)
```

1. Trips publiziert `TripCreated` — Expense erstellt eine lokale `TripProjection` mit Trip-Name und TenantId
2. Bei jedem `ParticipantJoinedTrip` wird der Teilnehmer zur TripProjection hinzugefuegt
3. `TripCompleted` loest die automatische Erstellung eines `Expense`-Aggregats aus
4. Alle Teilnehmer erhalten eine Standard-Gewichtung von 1.0
5. Das `ExpenseCreated`-Event wird nach Commit publiziert

## Szenario 4b: Beleg-Erfassung und Abrechnung

```
Browser        Gateway        Expense-SCS      PostgreSQL
  │               │               │               │
  │──GET /{tripId}▶              │               │
  │               │──Route────────▶               │
  │               │               │──find Expense─▶
  │◀──HTML (Expense-Detail)──────│               │
  │               │               │               │
  │──POST receipt─▶              │               │
  │               │──Route────────▶               │
  │               │               │──addReceipt()──▶
  │◀──HTML Fragment (HTMX)──────│               │
  │               │               │               │
  │──POST settle──▶              │               │
  │               │──Route────────▶               │
  │               │               │──expense.settle()
  │               │               │──save──────────▶
  │               │               │──ExpenseSettled (Event)
  │◀──Redirect────│               │               │
```

1. Organisator oeffnet die Abrechnungsseite fuer einen abgeschlossenen Trip
2. Belege werden mit Beschreibung, Betrag, Bezahlt-von und Datum erfasst (HTMX-Partials)
3. Gewichtungen koennen pro Teilnehmer angepasst werden (Erwachsener=1.0, Teilzeit=0.5, Kind<3=0.0)
4. Saldo-Berechnung: Fuer jeden Teilnehmer wird berechnet, was er bezahlt hat minus seinen gewichteten Anteil
5. Abschluss (settle): Status wechselt zu SETTLED, `ExpenseSettled`-Event wird publiziert

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
