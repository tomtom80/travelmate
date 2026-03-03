# ADR-0009: Maven Multi-Module Monorepo

## Status

Accepted

## Context

Travelmate besteht aus mehreren SCS (siehe ADR-0001), die gemeinsame Contracts (Event-Klassen, Shared Kernel) teilen. Separate Repositories pro SCS wuerden das Sharing von Code erschweren und die Versionierung von Event-Contracts verkomplizieren. Das Projekt wird von einem kleinen Team entwickelt.

## Decision

Wir verwenden ein Maven Multi-Module Monorepo mit folgender Struktur:

- `travelmate-parent` (Root POM) -- Dependency Management, Plugin-Konfiguration
- `travelmate-common` -- Shared Event-Contracts, gemeinsame Value Objects (z.B. TenantId)
- `travelmate-iam` -- IAM Self-Contained System
- `travelmate-trips` -- Trips Self-Contained System
- `travelmate-gateway` -- Spring Cloud Gateway

Jedes SCS-Modul ist ein eigenstaendiges Spring Boot Artefakt und separat deploybar. Das `travelmate-common`-Modul wird als Maven-Dependency eingebunden.

## Consequences

### Positive

- Event-Contracts in `travelmate-common` stellen Compile-Time-Kompatibilitaet sicher
- Einheitliches Dependency Management ueber den Parent POM
- Atomare Commits ueber mehrere Module moeglich (z.B. Event-Contract + Consumer)
- Einfacheres Onboarding: ein Repository, ein `git clone`
- Gemeinsame CI/CD-Pipeline moeglich

### Negative

- Groesseres Repository, laengere Clone-Zeiten mit der Zeit
- CI muss erkennen, welche Module sich geaendert haben (Selective Builds)
- Gefahr von ungewollter Kopplung ueber `travelmate-common`
- Merge-Konflikte bei paralleler Arbeit an mehreren Modulen wahrscheinlicher
