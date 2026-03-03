# ADR-0001: Self-Contained Systems als Architekturstil

## Status

Accepted

## Context

Travelmate ist eine Multi-Tenant Plattform zur Planung von Huettenurlaub mit mehreren fachlichen Domaenen (IAM, Trips, Expenses). Die einzelnen Domaenen sollen unabhaengig entwickelt, deployed und skaliert werden koennen. Ein klassischer Monolith wuerde zu enger Kopplung fuehren, waehrend feingranulare Microservices fuer ein kleines Team zu viel operativen Overhead erzeugen.

## Decision

Wir verwenden Self-Contained Systems (SCS) als Architekturstil. Jeder Bounded Context (z.B. `travelmate-iam`, `travelmate-trips`, `travelmate-expense`) ist ein eigenstaendiges System mit eigener UI (Thymeleaf + HTMX), eigenem Backend (Spring Boot) und eigener Datenbank (PostgreSQL). Die SCS kommunizieren untereinander ausschliesslich asynchron ueber Kafka Events. Es gibt keine synchronen Service-to-Service-Aufrufe.

## Consequences

### Positive

- Jedes SCS ist unabhaengig deploybar und skalierbar
- Teams koennen autonom an einem SCS arbeiten ohne Koordinationsaufwand
- Technologie-Entscheidungen koennen pro SCS getroffen werden
- Ausfaelle eines SCS beeinflussen andere SCS nicht direkt
- Klare fachliche Grenzen durch Alignment mit Bounded Contexts

### Negative

- UI-Konsistenz ueber SCS-Grenzen hinweg erfordert Disziplin (Shared CSS/Design Tokens)
- Daten-Redundanz durch lokale Projektion von Events aus anderen SCS
- Ein Gateway wird als zentraler Entry Point benoetigt (siehe ADR-0003)
- Hoehere initiale Komplexitaet im Vergleich zu einem Monolithen
