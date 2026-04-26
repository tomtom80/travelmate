# ADR-0024: Organizer-Rolle als Trip-lokale Aggregat-Eigenschaft

## Status
Akzeptiert (Iteration 18)

## Kontext

Trip-Aggregat besitzt bereits `organizerIds: List<UUID>` und `grantOrganizerRights(UUID)` (siehe `travelmate-trips/src/main/java/de/evia/travelmate/trips/domain/trip/Trip.java`). Backlog-Item E-IAM-05 (Multi-Organizer Role Management) verlangt eine Entscheidung, ob die Organizer-Rolle pro Trip (Aggregat-Eigenschaft) oder global pro Account (Keycloak-Rolle / IAM-Aggregat) verwaltet wird.

Heute existiert die Keycloak-Rolle `organizer` als Authorisierungsklammer fuer "darf ueberhaupt Trips anlegen". Ein zweiter "Organizer-eines-konkreten-Trips" ist davon zu unterscheiden.

## Entscheidung

Die operative Organizer-Rolle eines konkreten Trips bleibt eine **Trip-lokale Aggregat-Eigenschaft**. Die Keycloak-Rolle `organizer` bleibt ausschliesslich Berechtigungsklammer fuer das Anlegen von Trips. Promotion/Demotion innerhalb eines Trips erfolgt via Application-Service-Methoden:

- `TripService.grantOrganizerRole(TripId tripId, UUID actorId, UUID promotedAccountId)`
- `TripService.revokeOrganizerRole(TripId tripId, UUID actorId, UUID demotedAccountId)`

mit Aggregat-Invarianten:

1. `organizerIds.size() >= 1` zu jeder Zeit (Last-Organizer-Schutz)
2. Nur bestehender Organizer darf promoten/demoten (Self-Demotion erlaubt, solange ein anderer Organizer existiert)
3. promotedAccount muss Participant des Trips sein (verhindert Cross-Tenant-Promotions ueber Participant-Constraint)

Domain-Events:
- `OrganizerRoleGranted(tripId, accountId, occurredOn)` in `travelmate-common/events/trips/`
- `OrganizerRoleRevoked(tripId, accountId, occurredOn)` analog
- Routing-Keys: `trips.organizer-role-granted`, `trips.organizer-role-revoked`

## Konsequenzen

### Positiv
- Trip-Aggregat behaelt Owner-Schaft seiner Authorisierungsregeln (DDD-Konformitaet)
- Cross-Tenant-Promotions unmoeglich: Participant-Constraint impliziert gleiche Tenant-Domain
- Konsistent mit ADR-0008 (DDD/Hexagonal) und ADR-0001 (SCS)
- Keine Keycloak-Rollen-Explosion (sonst pro Trip eine Rolle)
- Einfache Erweiterbarkeit: Trip-Aggregat haelt alle Authorisierungsdaten lokal

### Negativ
- Keine Wiederverwendung des Organizer-Status ueber Trips hinweg: ein Account muss pro Trip einzeln zum Organizer ernannt werden
- Zwei "Organizer"-Begriffe im System (Keycloak-Rolle vs. Trip-Property), potenziell verwirrend — muss in `docs/arc42/12-glossary.md` gepflegt werden

## Alternativen (verworfen)

### A: Keycloak-Rolle `trip-organizer-{tripId}` pro Trip
- Vorteile: Single Source of Truth in Keycloak
- Nachteile: Rollen-Explosion in Keycloak; ueberschreitet das, wofuer Keycloak gebaut ist; Cross-Tenant-Lecks moeglich

### B: Eigenes Aggregat `TripMembership` in IAM
- Vorteile: explizite IAM-Verantwortung
- Nachteile: doppelte Datenhaltung Trips ↔ IAM; Cross-SCS-Synchronisation noetig; widerspricht ADR-0001 (SCS-Autonomie)

## Referenzen
- ADR-0001 (Self-Contained Systems)
- ADR-0008 (DDD + Hexagonal)
- ADR-0011 (Ubiquitous Language)
- Backlog: E-IAM-05 (Multi-Organizer Role Management)
