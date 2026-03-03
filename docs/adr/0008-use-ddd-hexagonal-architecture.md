# ADR-0008: DDD + Hexagonal Architecture pro SCS

## Status

Accepted

## Context

Travelmate ist eine fachlich komplexe Domaene (Tenants, Users, Roles, Trips, Expenses). Die Geschaeftslogik soll unabhaengig von Framework-Details testbar und wartbar sein. Jedes SCS (siehe ADR-0001) soll eine klare innere Struktur haben.

## Decision

Jedes SCS folgt Domain-Driven Design (DDD) mit Hexagonal Architecture (Ports & Adapters). Die Package-Struktur:

- `domain/` -- Reine Geschaeftslogik ohne Spring-Abhaengigkeiten (Entities, Value Objects, Repository-Interfaces als Ports, Domain Events)
- `application/` -- Use Cases als Application Services, Commands, Representations
- `adapters/` -- Infrastruktur-Implementierungen (Web, Persistence, Messaging, Security)

Java Records werden fuer Entities, Value Objects, Commands und Domain Events verwendet. Value Objects validieren sich selbst im Compact Constructor.

## Consequences

### Positive

- Domain-Layer ist framework-unabhaengig und isoliert testbar
- Klare Trennung von Geschaeftslogik und technischer Infrastruktur
- Ports & Adapters erlauben einfachen Austausch von Infrastruktur (z.B. In-Memory zu JPA)
- DDD-Patterns (Aggregates, Value Objects, Domain Events) foerdern fachlich korrekten Code
- Java Records erzwingen Immutability

### Negative

- Hoehere initiale Komplexitaet durch mehrere Schichten und Mappings
- Mehr Boilerplate durch Trennung von Domain- und Persistence-Modell
- DDD erfordert tiefes Verstaendnis der Fachdomaene
- Overhead bei einfachen CRUD-Szenarien
