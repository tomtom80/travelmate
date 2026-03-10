# ADR-0012: Trip-Einladung per E-Mail und Plattform-Einladung

## Status

Accepted

## Context

Die bisherige Einladungsfunktion in Travelmate hatte mehrere Luecken:

1. **Keine E-Mail bei Trip-Einladung**: Wenn ein Organisator ein Mitglied zu einer Reise einlaedt, erfaehrt das Mitglied davon nur, wenn es die Trip-Seite manuell oeffnet. Es wird keine Benachrichtigung versendet.
2. **Keine Einladung neuer Nutzer zu einem Trip**: Nur bestehende TravelParty-Mitglieder koennen zu einer Reise eingeladen werden. Ein Organisator kann niemanden einladen, der noch kein Travelmate-Konto hat.
3. **Geburtsdatum optional**: Das Geburtsdatum wird bei der Registrierung nicht verlangt und ist in der Datenbank nullable. Fuer Funktionen wie Essensplanung und Gewichtung bei der Abrechnung ist es jedoch notwendig.
4. **Generische Keycloak-E-Mail**: Die automatische E-Mail bei Mitgliedereinladung (executeActions) sagt nur "Aktion erforderlich", ohne zu erklaeren, warum der Nutzer eingeladen wurde.

## Decision

### 1. Geburtsdatum verpflichtend

`DateOfBirth` wird fuer alle Accounts und Dependents verpflichtend. Die Sign-Up-Seite und alle Einladungsformulare verlangen ein Geburtsdatum. Eine Flyway-Migration setzt bestehende NULL-Werte auf einen Platzhalter und macht die Spalte NOT NULL.

### 2. Trip-Einladungs-E-Mail via Spring Mail im Trips SCS

Das Trips SCS versendet Einladungs-E-Mails direkt ueber Spring Mail + Thymeleaf-Templates. Ein `InvitationEmailListener` reagiert auf `InvitationCreated`-Events (via `@TransactionalEventListener(AFTER_COMMIT)`) und rendert eine HTML-E-Mail mit Trip-Name, Zeitraum und Einlader-Name.

**Alternative verworfen**: Ein separates Notification SCS wurde erwogen, aber verworfen, da dies fuer den aktuellen Umfang zu viel Infrastruktur-Overhead bedeutet. Die SCS-Eigenstaendigkeit wird durch den lokalen Mail-Versand im Trips SCS gewahrt.

### 3. Externe Einladung ueber Event-Choreografie

Fuer die Einladung neuer Nutzer wird ein Cross-SCS Event-Flow verwendet:

```
Trips: InvitationService.inviteExternal()
  → Invitation [AWAITING_REGISTRATION]
  → publiziert InvitationCreated (fuer E-Mail an neuen Nutzer)
  → publiziert ExternalUserInvitedToTrip (fuer IAM Benutzererstellung)

IAM: ExternalInvitationConsumer
  → konsumiert ExternalUserInvitedToTrip
  → erstellt Keycloak-Benutzer + Account
  → publiziert AccountRegistered

Trips: TravelPartyService.onAccountRegistered()
  → aktualisiert TravelParty-Projektion
  → InvitationService.linkAwaitingInvitations()
  → Auto-Accept: Invitation → ACCEPTED, Participant wird zum Trip hinzugefuegt
```

**Auto-Join bei Registrierung**: Eingeladene neue Nutzer werden automatisch zum Trip hinzugefuegt (kein manuelles Akzeptieren). Dies vereinfacht den Onboarding-Flow fuer Nutzer, die explizit eingeladen wurden.

**Alternative verworfen**: Ein synchroner REST-Call von Trips nach IAM wurde erwogen, aber verworfen, da dies die SCS-Eigenstaendigkeit verletzen wuerde. Die asynchrone Event-Choreografie wahrt die lose Kopplung.

### 4. Keycloak-E-Mail mit Kontext

Die `executeActions`-E-Mail-Templates in Keycloak werden angepasst, um den Einladungskontext zu erklaeren ("Du wurdest als Mitglied einer Reisepartei zu Travelmate eingeladen").

## Consequences

### Positiv
- Nutzer werden per E-Mail ueber Trip-Einladungen informiert
- Organisatoren koennen Personen ohne Travelmate-Konto direkt einladen
- Vollstaendige Geburtsdaten fuer alle Nutzer ermoeglichen Folge-Features (Essensplanung, Gewichtung)
- SCS-Eigenstaendigkeit bleibt durch lokalen Mail-Versand und asynchrone Events gewahrt

### Negativ
- Invitation-Aggregat wird komplexer (zwei Typen: MEMBER, EXTERNAL; zusaetzlicher Status AWAITING_REGISTRATION)
- Eventual Consistency bei der externen Einladung: zwischen Einladung und Auto-Join koennen Sekunden vergehen
- Mail-Versand im Trips SCS bedeutet eine zusaetzliche Infrastruktur-Abhaengigkeit (SMTP-Server)

### Neue Events
- `InvitationCreated` (trips → intern, fuer E-Mail-Versand)
- `ExternalUserInvitedToTrip` (trips → IAM, fuer Benutzererstellung)

### Neue Infrastruktur
- Spring Mail im Trips SCS (`spring-boot-starter-mail`)
- Mailpit als Dev-SMTP-Server (bereits vorhanden)
- RabbitMQ-Queue `iam.external-user-invited` fuer IAM-Konsumenten

### Verschobene Stories
Die bisherigen Iteration-4-Stories (US-INFRA-020 ArchUnit, US-INFRA-021 JaCoCo, US-INFRA-022 DLQ, US-INFRA-030 Tenant-Isolation-Tests) werden auf Iteration 5 verschoben.
