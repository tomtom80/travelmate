# ADR-0025: Event-Versionierung und Naming-Konvention

## Status
Vorgeschlagen (Iteration 18)

## Kontext

`travelmate-common` haelt aktuell 19 Events (10 IAM, 9 Trips). Es existiert keine Versionierungsstrategie. Bei einem brechenden Schemawechsel (z.B. neues Pflichtfeld) wuerden Consumer mit unverstaendlichen Deserialisierungsfehlern in der DLQ landen.

Naming ist leicht inkonsistent: `AccommodationPriceSet` weicht vom Vergangenheitsform-Pattern (`*Created/*Updated/*Deleted`) ab.

## Entscheidung

### 1. Additive Aenderungen ohne Versionswechsel
Optionale Felder koennen jederzeit hinzugefuegt werden. Consumer ignorieren unbekannte Felder durch Jackson-Default `FAIL_ON_UNKNOWN_PROPERTIES = false`.

### 2. Brechende Aenderungen via Klassen-Suffix `V2`
Z.B. `TripCreatedV2` mit neuem Pflichtfeld. Producer publiziert beide Versionen waehrend Migration auf parallelen Routing-Keys (`trips.trip-created.v2`); Consumer migrieren in eigener Geschwindigkeit.

### 3. Naming-Konvention
Event-Records enden ausschliesslich auf:
`*Created`, `*Updated`, `*Deleted`, `*Granted`, `*Revoked`, `*Joined`, `*Removed`, `*Completed`, `*Cancelled`.

`*Set` ist deprecated. `AccommodationPriceSet` wird in Folgeschritten zu `AccommodationPriceUpdated` umbenannt (mit Versionsuebergangsphase per Punkt 2).

### 4. Pflichtfelder fuer alle Events
- `tenantId: UUID`
- `occurredOn: LocalDate`

### 5. Durchsetzung via ArchUnit
ArchUnit-Tests im common-Modul setzen Naming und Pflichtfelder durch. Pilot in Trips, danach in IAM/Expense ausgerollt (Iter 19).

## Konsequenzen

### Positiv
- Schema-Evolution explizit dokumentiert
- Keine externe Schema-Registry noetig (passt zu SCS-Philosophie)
- Naming-Drift wird im Build sichtbar
- Consumer koennen unabhaengig migrieren

### Negativ
- Migration bestehender Events (`AccommodationPriceSet`) ist Folgeaufwand
- Zwei parallele Eventversionen waehrend Migration erhoehen Komplexitaet kurzzeitig
- Producer muessen beide Versionen publizieren waehrend Uebergangsphase

## Alternativen (verworfen)

### A: Schema-Registry (Apicurio, Confluent)
Overkill fuer aktuelle Eventgroesse (19 Events) und Anzahl Producers/Consumers. Erhoeht Infrastructure-Komplexitaet.

### B: Keine Versionierung
Implizite Annahme dass alle Consumer immer gemeinsam migriert werden — heute richtig, langfristig unsicher und blockiert unabhaengige SCS-Releases.

## Referenzen
- ADR-0006 (RabbitMQ als Messaging-Backbone)
- https://www.enterpriseintegrationpatterns.com/patterns/messaging/MessageVersioning.html
- Iteration-18-Architektur-Report Sektion 2 + 6
