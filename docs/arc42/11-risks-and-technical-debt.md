# 11. Risiken und technische Schulden

## Risiken

| Risiko | Eintrittswahrscheinlichkeit | Auswirkung | Maßnahme |
|--------|----------------------------|------------|----------|
| **Keycloak-Komplexität** | Hoch | Mittel | Keycloak ist mächtig, aber komplex in Konfiguration und Betrieb. Realm-Konfiguration, Theme-Anpassung und Multi-Tenancy erfordern tiefes Wissen. | Schrittweise Konfiguration, gute Dokumentation der Realm-Einstellungen, Automatisierung via Keycloak Admin CLI |
| **SCS-Boundary-Evolution** | Mittel | Hoch | Die Grenzen der Bounded Contexts können sich im Laufe der Entwicklung als suboptimal herausstellen. Insbesondere die Abgrenzung zwischen Trips und Expense könnte sich verschieben. | Context Mapping regelmäßig überprüfen, Event-Verträge versionieren, Refactoring frühzeitig einplanen |
| **PWA-Limitierungen** | Mittel | Mittel | PWAs haben Einschränkungen gegenüber nativen Apps (z.B. iOS-Restriktionen für Service Worker, eingeschränkter Kamera-Zugriff, kein Push auf iOS). | Graceful Degradation, kritische Funktionen auch online nutzbar, iOS-Einschränkungen dokumentieren |
| **RabbitMQ-Betriebskomplexität** | Niedrig | Mittel | RabbitMQ ist betrieblich einfach, aber Monitoring, Exchange/Queue-Management und Routing erfordern Erfahrung. | RabbitMQ Management UI für Monitoring, klare Routing-Key-Konvention, Dead Letter Queue für fehlerhafte Events |
| **Kleine Teamgröße** | Hoch | Mittel | Ein kleines Team muss drei SCS plus Infrastruktur betreuen. Wissenssilos und Bus-Faktor sind Risiken. | Gemeinsame Architekturentscheidungen (ADRs), Code Reviews, Pair Programming, gute Dokumentation |

## Technische Schulden

| Schuld | Beschreibung | Priorität |
|--------|-------------|-----------|
| ~~**In-Memory-Repositories**~~ | ~~Einige Repository-Implementierungen sind noch In-Memory statt PostgreSQL~~ — **Geloest in Iteration 2**: IAM hat vollstaendige JPA-Persistence-Adapter | ~~Hoch~~ |
| **Fehlende Expense-SCS-Implementierung** | Der Expense Bounded Context ist noch nicht implementiert | Mittel |
| **Security im Test-Profil deaktiviert** | Integrationstests testen nicht die Security-Konfiguration | Niedrig |
| ~~**Fehlende End-to-End-Tests**~~ | ~~Keine automatisierten E2E-Tests ueber die SCS-Grenzen hinweg~~ — **Adressiert**: E2E-Modul mit Playwright angelegt (siehe ADR-0010), Grundstruktur vorhanden | ~~Mittel~~ |
| **Monitoring und Observability** | Kein zentrales Logging, Tracing oder Metriken eingerichtet | Mittel |
| **Kubernetes-Manifeste unvollständig** | Deployment-Manifeste nur für IAM vorhanden, nicht für alle SCS | Mittel |
