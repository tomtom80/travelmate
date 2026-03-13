# 8. Querschnittliche Konzepte

## Multi-Tenancy

Jedes Aggregat ist durch eine `TenantId` scoped. Die Mandantentrennung ist ein zentrales Architekturprinzip:

- **HTTP-Header:** Tenant-Identifikation über den `x-travelmate-tenant-id` Header
- **TenantIdentificationFilter:** Extrahiert die Tenant-ID aus dem Request
- **TenantContext:** Speichert die aktuelle Tenant-ID in einem `ThreadLocal`
- **Datenisolierung:** Alle Datenbankabfragen filtern nach `TenantId`

## Security (OIDC)

- **Keycloak** als zentraler Identity Provider
- **OIDC-Flow** über Spring Security
- **Rollenmodell:** `role/trips.organizer`, `role/trips.participant`
- **Form Login + HTTP Basic** für API-Zugriffe (Entwicklung)
- **SAML2** als alternative Authentifizierungsmethode via Keycloak
- **CORS:** Konfiguriert für `localhost:3000` (Entwicklung)
- **Test-Profil:** Security ist im `test`-Profil deaktiviert

## Event-basierte Integration (travelmate-common)

Die asynchrone Kommunikation zwischen SCS basiert auf Domain Events ueber RabbitMQ:

- **DomainEvent-Interface:** Definiert `occurredOn(): LocalDate`
- **RabbitMQ Topic Exchange:** `travelmate.events` mit Routing Keys in `RoutingKeys.java`
- **JSON-Serialisierung:** Spring AMQP mit Jackson2JsonMessageConverter
- **Event-Vertraege:** Gemeinsame Event-Definitionen in `travelmate-common` (Maven-Modul)
- **Idempotenz:** Consumer muessen idempotent mit wiederholten Events umgehen
- **Event-Publishing:** Domain-Events werden nach `repository.save()` ueber Spring `ApplicationEventPublisher` veroeffentlicht. Ein `@TransactionalEventListener(AFTER_COMMIT)` leitet an RabbitMQ weiter.

### IAM Events

| Event | Routing Key | Beschreibung |
|-------|------------|--------------|
| `AccountRegistered` | `iam.account-registered` | Account wurde registriert |
| `MemberAddedToTenant` | `iam.member-added` | Mitglied zu Tenant hinzugefuegt |
| `DependentAddedToTenant` | `iam.dependent-added` | Mitreisender hinzugefuegt |

### Trips Events

| Event | Routing Key | Beschreibung |
|-------|------------|--------------|
| `TripCreated` | `trips.trip-created` | Trip wurde erstellt |
| `ParticipantJoinedTrip` | `trips.participant-confirmed` | Teilnehmer bestaetigt |
| `TripCompleted` | `trips.trip-completed` | Trip abgeschlossen |
| `InvitationCreated` | `trips.invitation-created` | Einladung erstellt (loest E-Mail aus) |
| `ExternalUserInvitedToTrip` | `trips.external-user-invited` | Externe Einladung (IAM erstellt User) |

### Expense Events

| Event | Routing Key | Beschreibung |
|-------|------------|--------------|
| `ExpenseCreated` | `expense.expense-created` | Abrechnung fuer abgeschlossenen Trip erstellt |
| `ExpenseSettled` | `expense.expense-settled` | Abrechnung abgeschlossen |

### Event-Flow

```
IAM                         RabbitMQ             Trips / Expense
 │                               │                      │
 │──AccountRegistered───────────▶│──────────────────────▶│  → TravelParty anlegen
 │──DependentAddedToTenant──────▶│──────────────────────▶│  → Dependent in Party
 │──RoleAssignedToUser──────────▶│──────────────────────▶│  → Rolle aktivieren
```

```
Trips                       RabbitMQ             Expense
 │                               │                   │
 │──TripCreated─────────────────▶│──────────────────▶│  → TripProjection anlegen
 │──ParticipantJoinedTrip───────▶│──────────────────▶│  → Teilnehmer in Projektion
 │──TripCompleted───────────────▶│──────────────────▶│  → Expense erstellen (Gewichtungen 1.0)
```

## E-Mail-Kommunikation (ADR-0012)

Die E-Mail-Kommunikation folgt dem Prinzip der SCS-Eigenstaendigkeit:

- **Keycloak-E-Mails:** Verifizierung, Passwort-Reset und Account-Einrichtung werden direkt von Keycloak ueber angepasste Theme-Templates versendet (Realm `travelmate`, Theme `travelmate`).
- **Trip-Einladungs-E-Mails:** Das Trips SCS versendet Einladungs-E-Mails eigenstaendig via Spring Mail + Thymeleaf-Templates. Ein `InvitationEmailListener` (`@TransactionalEventListener(AFTER_COMMIT)`) reagiert auf `InvitationCreated`-Events.
- **SMTP-Server:** Mailpit als Development-SMTP-Server (Port 1025, Web UI Port 8025). In Tests deaktiviert (Port 0).
- **Enriched Events:** `InvitationCreated` wird im Application Service mit Trip-Name, Zeitraum und Einlader-Name angereichert, damit der E-Mail-Adapter alle Informationen hat.

## Validierung (Assertion-Utility)

- Value Objects validieren sich selbst im Compact Constructor (Java Records)
- Zentrale `Assertion`-Utility-Klasse für konsistente Validierungsmeldungen
- Validierung erfolgt in der Domain-Schicht, nicht in Adaptern

```java
public record TenantName(String value) {
    public TenantName {
        Assertion.assertNotEmpty(value, "TenantName must not be empty");
    }
}
```

## Progressive Web App (PWA)

- **Service Worker:** Caching-Strategien für Offline-Fähigkeit
- **Web App Manifest:** Installation auf dem Homescreen
- **Mobile-First Design:** Responsives Layout optimiert für Smartphones
- **Offline-Unterstützung:** Kritische Daten lokal verfügbar (z.B. Einkaufsliste)

## Persistenz

- Jedes SCS besitzt eine eigene PostgreSQL-Datenbank
- Repository-Interfaces werden in der Domain-Schicht definiert (Ports)
- Implementierungen befinden sich im Adapter-Layer (Persistence)
- Entwicklungsphase: Teilweise In-Memory-Implementierungen

## OpenAPI-Dokumentation

- **SpringDoc OpenAPI** generiert automatisch API-Spezifikationen
- **Swagger UI** verfügbar unter `/swagger-ui.html`
- API-Spezifikationen werden beim `package`-Build in das `openapi/`-Verzeichnis geschrieben

## Referenzen

![API Design](../../design/evia.team.orc.thomas-klingler%20-%20API%20Design.jpg)

![Storage Characteristics](../../design/evia.team.orc.thomas-klingler%20-%20Define%20Storage%20characteristics.jpg)
