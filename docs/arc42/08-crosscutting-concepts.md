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

Die asynchrone Kommunikation zwischen SCS basiert auf Domain Events:

- **DomainEvent-Interface:** Definiert `occurredOn(): LocalDate`
- **Kafka Topics:** `role-assigned`, `role-unassigned`
- **JSON-Serialisierung:** Spring Kafka mit Jackson
- **Event-Verträge:** Gemeinsame Event-Definitionen in `travelmate-common` (Maven-Modul)
- **Idempotenz:** Consumer müssen idempotent mit wiederholten Events umgehen

### Event-Flow

```
IAM                         Trips / Expense
 │                               │
 │──RoleAssignedToUser──────────▶│  → Organizer/Participant aktivieren
 │──RoleUnassignedFromUser──────▶│  → Organizer/Participant deaktivieren
```

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
