# 12. Glossar

## Ubiquitous Language

| Begriff (EN) | Begriff (DE) | Beschreibung |
|-------------|-------------|-------------|
| **Tenant** | Mandant | Eine isolierte Einheit im System, z.B. eine Familie oder Freundesgruppe. Jeder Mandant hat eigene Benutzer, Trips und Abrechnungen. |
| **Trip** | Hüttenurlaub / Reise | Ein geplanter Hüttenurlaub mit Zeitraum, Unterkunft, Teilnehmern und Mahlzeiten. |
| **Organizer** | Organisator | Ein Benutzer mit der Rolle, Trips zu erstellen und zu verwalten. |
| **Participant** | Teilnehmer | Ein Benutzer, der an einem Trip teilnimmt. |
| **MealPlan** | Essensplan | Der Gesamtplan für alle Mahlzeiten eines Trips (7 Frühstück + 7 Abendessen). |
| **Meal** | Mahlzeit | Eine einzelne Mahlzeit, entweder Frühstück oder Abendessen. |
| **Ingredient** | Zutat | Eine Zutat für ein Gericht innerhalb einer Mahlzeit. |
| **ShoppingList** | Einkaufsliste | Automatisch generierte Liste aller benötigten Zutaten für den Trip. |
| **Expense** | Abrechnung | Die Gesamtabrechnung aller Kosten eines Trips. |
| **Receipt** | Beleg / Bon | Ein einzelner Kassenbeleg mit Betrag und optionalem Foto. |
| **Weighting** | Gewichtung | Faktor für die Kostenaufteilung pro Person: 1.0 = Erwachsener, 0.5 = Teilzeit-Teilnehmer, 0.0 = Kind unter 3 Jahren. |
| **Settlement** | Abrechnung / Saldo | Der berechnete Saldo pro Familie nach Verrechnung aller Belege und Gewichtungen. |
| **Accommodation** | Unterkunft / Hütte | Die gebuchte Unterkunft für den Trip. |
| **LocationPoll** | Standort-Abstimmung | Eine Abstimmung unter den Teilnehmern zur Auswahl der Unterkunft. |
| **DownPayment** | Anzahlung | Eine Vorauszahlung für den Trip (z.B. Hütten-Buchung). |
| **Policy** | Rollenzuweisung | Die Zuordnung einer Rolle zu einem Benutzer (User-Role-Mapping). |
| **Group** | Gruppe | Eine Zusammenfassung von Benutzern, z.B. eine Familie innerhalb eines Mandanten. |
| **Role** | Rolle | Eine fachliche Berechtigung im System, z.B. `organizer` oder `participant`. |
| **DateRange** | Zeitraum | Der Reisezeitraum eines Trips, definiert durch An- und Abreisedatum. |

## Technische Begriffe

| Begriff | Beschreibung |
|---------|-------------|
| **SCS (Self-Contained System)** | Ein eigenständiges System mit eigener UI, Datenbank und Fachlogik. Travelmate besteht aus drei SCS. |
| **Bounded Context** | Eine fachliche Grenze innerhalb derer ein einheitliches Domänenmodell gilt (DDD-Konzept). |
| **Hexagonale Architektur** | Architekturmuster mit Ports (Schnittstellen) und Adapters (Implementierungen), das die Domain von der Infrastruktur trennt. |
| **Domain Event** | Ein fachliches Ereignis, das eine Zustandsänderung in einem Bounded Context signalisiert und asynchron an andere Kontexte weitergegeben wird. |
| **TenantId** | Technischer Schlüssel zur Mandantentrennung, der in jedem Aggregat enthalten ist. |
| **OIDC** | OpenID Connect — Authentifizierungsprotokoll auf Basis von OAuth 2.0, implementiert über Keycloak. |
| **KRaft** | Kafka Raft — Betriebsmodus von Apache Kafka ohne separaten Zookeeper-Cluster. |
| **PWA** | Progressive Web App — Webanwendung mit App-ähnlichem Verhalten und Offline-Fähigkeit. |
