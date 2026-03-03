# ADR-0006: Kafka Events fuer Inter-SCS-Kommunikation

## Status

Accepted

## Context

Die SCS (siehe ADR-0001) muessen Informationen austauschen, z.B. wenn im IAM-Service eine Rolle zugewiesen wird, muss der Trips-Service einen Organizer oder Participant aktivieren. Synchrone REST-Aufrufe zwischen SCS wuerden enge Kopplung und Verfuegbarkeitsabhaengigkeiten erzeugen.

## Decision

Wir verwenden Apache Kafka fuer die asynchrone Event-basierte Kommunikation zwischen SCS. Kafka laeuft im KRaft-Modus (ohne Zookeeper). Topics folgen der Namenskonvention `travelmate.<scs>.<event>` (z.B. `travelmate.iam.role-assigned`, `travelmate.iam.role-unassigned`). Events werden als JSON serialisiert. Event-Contracts werden im gemeinsamen `travelmate-common`-Modul definiert (siehe ADR-0009).

## Consequences

### Positive

- Lose Kopplung zwischen SCS: Producer und Consumer sind unabhaengig
- Ausfallsicherheit: Events werden persistent gespeichert und koennen nachgeholt werden
- KRaft-Modus vereinfacht die Infrastruktur (kein Zookeeper noetig)
- Event-getriebene Architektur passt zur DDD-Philosophie (Domain Events)
- Skalierbar durch Partitionierung

### Negative

- Eventual Consistency: Daten sind nicht sofort in allen SCS konsistent
- Debugging ueber SCS-Grenzen hinweg ist komplexer (Distributed Tracing noetig)
- Kafka als Infrastruktur-Komponente muss betrieben und ueberwacht werden
- Event-Schema-Evolution erfordert Disziplin (Backward Compatibility)
