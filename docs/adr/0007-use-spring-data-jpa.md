# ADR-0007: Spring Data JPA fuer Persistenz

## Status

Accepted

## Context

Jedes SCS benoetigt eine eigene Datenbank (siehe ADR-0001). Die Domaenenmodelle bestehen aus Aggregates mit verschachtelten Value Objects, die relational persistiert werden muessen. Das Team ist mit JPA/Hibernate vertraut.

## Decision

Wir verwenden Spring Data JPA mit PostgreSQL als Datenbank und Flyway fuer Schema-Migrationen. Jedes SCS hat eine eigene PostgreSQL-Datenbank-Instanz (Database-per-Service). JPA Entities liegen im Adapter-Layer (`adapters/persistence`) und werden auf die Domain-Entities gemappt, um die Hexagonal Architecture (siehe ADR-0008) einzuhalten. Flyway-Migrationen liegen unter `src/main/resources/db/migration`.

## Consequences

### Positive

- Spring Data JPA reduziert Boilerplate durch Repository-Interfaces
- PostgreSQL ist ausgereift, zuverlaessig und kostenlos
- Flyway garantiert reproduzierbare und versionierte Schema-Migrationen
- Database-per-Service stellt fachliche Isolation sicher
- Gute IDE-Unterstuetzung und breites Oekosystem

### Negative

- JPA-Mapping von DDD Aggregates kann komplex werden (Value Objects, Embeddables)
- Mapping zwischen JPA Entities und Domain Entities erzeugt Boilerplate
- PostgreSQL pro SCS erhoet den Infrastruktur-Aufwand
- Lazy Loading und N+1-Probleme erfordern Aufmerksamkeit
