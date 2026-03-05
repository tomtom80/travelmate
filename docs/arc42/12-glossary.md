# 12. Glossar

## Ubiquitous Language (ADR-0011)

UI verwendet Fachsprache, Code verwendet technische Namen. Siehe ADR-0011 fuer die vollstaendige Begriffsmatrix.

| Fachbegriff (DE) | Fachbegriff (EN) | Technisch (Code) | Kontext | Beschreibung |
|---|---|---|---|---|
| **Reisepartei** | Travel Party | Tenant | IAM | Registrierungseinheit: Einzelperson, Paar oder Familie |
| **Mitglied** | Member | Account | IAM | Person mit eigenem Login, plant aktiv mit |
| **Mitreisende(r)** | Companion | Dependent | IAM | Person ohne Login, reist mit (Kind, Partner der nicht plant) |
| **Reise** | Trip | Trip | Trips | Ein konkreter Urlaub/Event mit Zeitraum |
| **Reisegruppe** | Travel Group | — | Trips | Alle Teilnehmer einer Reise (entsteht durch Einladungen) |
| **Einladung** | Invitation | Invitation | Trips | Einladung einer Reisepartei zu einer Reise |
| **Organisator** | Organizer | — | Trips | Mitglied das eine Reise erstellt und verwaltet |
| **Teilnehmer** | Participant | Participant | Trips | Reisepartei die an einer Reise teilnimmt |
| **Aufenthaltsdauer** | Stay Period | StayPeriod | Trips | Individueller An-/Abreisezeitraum eines Teilnehmers innerhalb des Trip-Zeitraums |
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
| **RabbitMQ** | Message Broker fuer asynchrone Kommunikation zwischen SCS via AMQP (Topic Exchange `travelmate.events`). |
| **PWA** | Progressive Web App — Webanwendung mit App-ähnlichem Verhalten und Offline-Fähigkeit. |
