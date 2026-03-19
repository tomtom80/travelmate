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
| `ParticipantJoinedTrip` | `trips.participant-confirmed` | Teilnehmer bestaetigt (erweitert um `participantTenantId` und `partyName` seit Iteration 9) |
| `TripCompleted` | `trips.trip-completed` | Trip abgeschlossen |
| `InvitationCreated` | `trips.invitation-created` | Einladung erstellt (loest E-Mail aus) |
| `ExternalUserInvitedToTrip` | `trips.external-user-invited` | Externe Einladung (IAM erstellt User) |
| `AccommodationPriceSet` | `trips.accommodation.price-set` | Unterkunftspreis gesetzt/geaendert (Expense aktualisiert TripProjection) |

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
 │──ParticipantJoinedTrip───────▶│──────────────────▶│  → Teilnehmer in Projektion (+ partyTenantId/partyName)
 │──TripCompleted───────────────▶│──────────────────▶│  → Expense erstellen (Gewichtungen 1.0)
 │──AccommodationPriceSet───────▶│──────────────────▶│  → accommodationTotalPrice in Projektion
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

## HTMX-Polling fuer Echtzeit-Updates (Iteration 8)

Da das Trips-SCS Servlet-basiert ist (kein WebFlux/SSE), werden Echtzeit-Updates ueber HTMX-Polling realisiert:

- **Polling-Intervall:** Alle 5 Sekunden via `hx-trigger="every 5s"` auf der Einkaufslistenseite
- **Automatische Deaktivierung:** Polling wird fuer COMPLETED/CANCELLED Trips deaktiviert (kein unnötiger Traffic)
- **Partial-Rendering:** Nur das Listenfragment wird aktualisiert, nicht die gesamte Seite
- **Anwendungsfall:** Mehrere Teilnehmer kaufen gleichzeitig im Supermarkt ein und sehen Status-Aenderungen der anderen in Echtzeit

```html
<div hx-get="/{tripId}/shopping-list/items"
     hx-trigger="every 5s"
     hx-swap="innerHTML">
  <!-- Shopping-Items werden alle 5s neu geladen -->
</div>
```

## Progressive Web App (PWA)

- **Web App Manifest:** `manifest.json` im Gateway mit App-Name, Icons und Theme-Color (Iteration 9)
- **App-Icons:** Bereitgestellt in verschiedenen Groessen fuer Homescreen-Installation
- **Mobile-First Design:** Responsives Layout optimiert fuer Smartphones (PicoCSS 2)
- **Offline-Unterstuetzung:** Geplant fuer zukuenftige Iterationen (z.B. Einkaufsliste offline)

## Reisepartei-Abrechnung (Party Settlement, Iteration 9)

Die Kostenabrechnung erfolgt auf zwei Ebenen:

1. **Individuelle Ebene:** Jeder Teilnehmer hat einen Saldo basierend auf Belegen und Gewichtungen (wie bisher)
2. **Reisepartei-Ebene:** `PartySettlement` aggregiert individuelle Salden nach `partyTenantId`

- **Datenherkunft:** `ParticipantJoinedTrip`-Event wurde um `participantTenantId` und `partyName` erweitert
- **Gruppierung:** Alle Teilnehmer einer Reisepartei werden zu einem Partei-Saldo zusammengefasst
- **Transfer-Berechnung:** Greedy-Algorithmus minimiert die Anzahl der Ueberweisungen zwischen Parteien
- **Vorauszahlungen:** `AdvancePaymentSuggestion` berechnet gerundete Vorschlaege: `ceil(accommodationCost / partyCount / 50) * 50`

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
