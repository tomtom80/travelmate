# Product Backlog - Travelmate

Stand: 2026-03-05

## Subdomain-Klassifikation (Strategic DDD)

| Subdomain | Typ | Bounded Context | Beziehung |
|-----------|-----|-----------------|-----------|
| Trips | **Core** | travelmate-trips | Upstream zu Expense (Partnership) |
| IAM | Supporting | travelmate-iam | Upstream zu Trips (U/D Conformist) |
| Expense | Generic | travelmate-expense | Downstream von Trips (Partnership) |

## Context Map

```
IAM ──(U)──→ D. Conformist ──→ Trips ──→ Partnership ──→ Expense
```

- **IAM -> Trips**: Trips ist Downstream Conformist. Trips konsumiert IAM-Events (AccountRegistered, DependentAddedToTenant) und baut lokale TravelParty-Projektionen.
- **Trips -> Expense**: Partnership. Expense konsumiert Trips-Events (TripCreated, ParticipantJoinedTrip, TripCompleted) fuer Ledger/Abrechnung.

---

## Epics & User Stories

### Epic 1: IAM - Basis (Iteration 2 - DONE)
- [x] US-1.1: Reisegruppe (Tenant) erstellen (Admin-UI)
- [x] US-1.2: Teilnehmer registrieren (Account mit manueller KeycloakUserId)
- [x] US-1.3: Mitreisende Kinder hinzufuegen (Dependent)
- [x] US-1.4: Events publizieren (AccountRegistered, DependentAddedToTenant)

### Epic 1b: IAM - Sign-up & Login (Iteration 3, Voraussetzung fuer Trips)
- [ ] US-1.5: Self-Service Sign-up: Reisegruppe anlegen + Admin-Account erstellen
  - Oeffentliche Sign-up-Seite (kein Login erforderlich)
  - Formular: Reisegruppen-Name, Vorname, Nachname, E-Mail, Passwort
  - Keycloak-User wird automatisch via Keycloak Admin API provisioniert
  - Tenant + Account werden in einem Vorgang erstellt
  - Admin erhaelt Organizer-Rolle automatisch
- [ ] US-1.6: Login mit erstellter Reisegruppe
  - Nach Sign-up automatisch eingeloggt (OIDC Session)
  - Bestehendes OIDC-Login via Gateway + Keycloak
  - Multi-Tenant: Benutzer ist einem Tenant zugeordnet, Tenant-Kontext wird aus JWT abgeleitet
- [ ] US-1.7: Weitere Mitglieder einladen (E-Mail-Einladung)
  - Organisator laedt neue Mitglieder per E-Mail ein
  - Eingeladener erhaelt Link, registriert sich mit Passwort
  - Keycloak-User wird bei Registrierung provisioniert
  - Account wird dem bestehenden Tenant zugeordnet
- [ ] US-1.8: Mitglieder-Verwaltung im Tenant-Kontext
  - Alle Mitglieder der Reisegruppe sehen und verwalten
  - Kinder/Dependents werden von ihren Eltern (Account-Inhabern) verwaltet
  - Nur Benutzer mit Account koennen sich einloggen

### Epic 2: Trips Core - Reiseplanung (Iteration 3)
- [ ] US-2.1: Reisegruppen-Projektion aus IAM-Events (TravelParty)
- [ ] US-2.2: Reise erstellen (Trip) mit Name, Zeitraum, Beschreibung
- [ ] US-2.3: Teilnehmer zu einer Reise einladen (Invitation)
- [ ] US-2.4: Einladung annehmen / ablehnen
- [ ] US-2.5: Aufenthaltsdauer pro Teilnehmer festlegen
- [ ] US-2.6: Reise-Status-Lifecycle (PLANNING -> CONFIRMED -> IN_PROGRESS -> COMPLETED)
- [ ] US-2.7: Unterkunft / Location mit Details (Zimmeranzahl, Preis, Infos)
- [ ] US-2.8: Location-Infos aus URL auslesen (Web Scraping / Metadaten)
- [ ] US-2.9: Unterkunft-Abstimmung (LocationPoll)

### Epic 3: Trips Extended - Mahlzeiten & Einkauf (Iteration 4)

#### Essensplan & Rezepte
- [ ] US-3.1: Essensplan fuer den Reise-Zeitraum erstellen (MealPlan)
  - Tage + Mahlzeiten (Fruehstueck, Mittag, Abend) als Raster
  - Essen aussetzen moeglich (MealSlot -> SKIP)
  - "Essen gehen" einplanen (MealSlot -> EATING_OUT, optional: Restaurant-Name)
- [ ] US-3.2: Essensrezept eingeben (Recipe)
  - Manuell: Name, Beschreibung, Portionen, Zutaten mit Menge+Einheit
  - Aus URL einlesen: Rezept-URL eingeben, Structured Data (schema.org/Recipe) extrahieren
- [ ] US-3.3: Rezept einem MealSlot im Essensplan zuordnen
  - Ein MealSlot referenziert ein Recipe
  - Aenderungen am Essensplan (neues Rezept) fliessen automatisch in die Einkaufsliste

#### Einkaufsliste (geteiltes Element des Events/Trips)
- [ ] US-3.4: Einkaufsliste pro Trip (ShoppingList)
  - Geteilt mit allen Teilnehmern (jeder sieht und bearbeitet dieselbe Liste)
  - Zwei Quellen fuer Eintraege:
    1. **Automatisch aus Rezepten**: Zutaten aller Rezepte im Essensplan werden aggregiert, gleiche Zutaten zusammengefasst, Mengen anhand Teilnehmerzahl skaliert
    2. **Manuell hinzugefuegt**: Snacks, Getraenke, Haushaltsartikel etc. — alles was mit der Allgemeinheit abgerechnet wird
  - Eintrag: Name, Menge, Einheit, Quelle (RECIPE / MANUAL), Status (OPEN / ASSIGNED / PURCHASED)
- [ ] US-3.5: Einkaufsposten zuweisen und erledigen
  - Teilnehmer kann sich Eintraege der Einkaufsliste selbst zuweisen ("Ich kuemmere mich drum")
  - Zugewiesener Eintrag zeigt den Teilnehmer-Namen an
  - Eintrag als "eingekauft" markieren (PURCHASED)
  - Andere Teilnehmer sehen den Status in Echtzeit (HTMX polling oder SSE)
- [ ] US-3.6: Bring-App-Integration
  - Einkaufsliste (oder Teilmenge: eigene zugewiesene Posten) an Bring-App uebertragen (Bring API)
  - Sync: Neue Eintraege in Travelmate -> Bring-App aktualisieren

### Epic 4: Expense - Abrechnung (Iteration 5)
- [ ] US-4.1: Ledger pro Reise anlegen (aus TripCreated-Event)
- [ ] US-4.2: Unterkunftskosten erfassen
- [ ] US-4.3: Einkaufsbelege erfassen (Receipt)
- [ ] US-4.4: Gewichtung pro Teilnehmer definieren (Erwachsener=1.0, Kind<3=0.0)
- [ ] US-4.5: Saldo berechnen (Settlement)
- [ ] US-4.6: Zahlungsaufforderung / Rueckzahlung

### Epic 5: Querschnittlich
- [ ] US-5.1: i18n (Deutsch + Englisch), Sprachwechsel zur Laufzeit
- [ ] US-5.2: README mit Developer-Dokumentation
- [ ] US-5.3: Arc42-Dokumentation aktualisieren
- [ ] US-5.4: E2E-Tests fuer alle Use Cases

---

## Ubiquitous Language (Deutsch -> Domain)

| Fachsprache (DE) | Domain Term | Bounded Context |
|------------------|-------------|-----------------|
| Reisegruppe | TravelParty | Trips |
| Reise / Huettenurlaub | Trip | Trips |
| Organisator | Organizer | Trips |
| Teilnehmer | Participant | Trips |
| Kind (Mitreisende) | Dependent / Child | Trips (projected from IAM) |
| Eltern / Erziehungsberechtigter | Guardian | Trips (projected from IAM) |
| Einladung | Invitation | Trips |
| Aufenthaltsdauer | StayPeriod | Trips |
| Unterkunft | Accommodation | Trips |
| Abstimmung | LocationPoll | Trips |
| Mahlzeitenplan | MealPlan | Trips |
| Essen | Meal | Trips |
| Zutat | Ingredient | Trips |
| Einkaufsliste | ShoppingList | Trips |
| Einkaufsposten | ShoppingItem | Trips |
| Einkaufsposten zuweisen | ShoppingItem.assignTo(participant) | Trips |
| Eingekauft | ShoppingItem -> PURCHASED | Trips |
| Snacks / Getraenke / Sonstiges | ShoppingItem (source=MANUAL) | Trips |
| Essensplan-Zeitfenster | MealSlot (day + meal type) | Trips |
| Essen aussetzen | MealSlot -> SKIP | Trips |
| Essen gehen | MealSlot -> EATING_OUT | Trips |
| Registrierung / Sign-up | SignUp | IAM |
| Mitglieder-Einladung | MemberInvitation | IAM |
| Rezept | Recipe | Trips |
| Rezept aus URL importieren | RecipeImport (URL scraping) | Trips |
| Bring-App Einkaufsliste | BringShoppingList | Trips (Integration) |
| Beleg / Kassenbon | Receipt | Expense |
| Gewichtung | Weighting | Expense |
| Abrechnung | Settlement | Expense |
| Kosten | Costs | Expense |
