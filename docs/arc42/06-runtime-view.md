# 6. Laufzeitsicht

## Szenario 1: Login-Flow (OIDC)

```
Browser        Gateway        IAM-SCS        Keycloak
  │               │              │               │
  │──GET /────────▶              │               │
  │               │──Redirect────▶               │
  │               │              │──Auth Request──▶
  │◀──────────────────────────────────Login Page──│
  │──Credentials──────────────────────────────────▶
  │               │              │◀─ID Token──────│
  │               │              │──Validate Token─▶
  │◀──Session + Redirect─────────│               │
```

1. Benutzer ruft die Anwendung auf
2. Gateway leitet an den IAM-SCS weiter
3. Spring Security erkennt fehlende Authentifizierung und leitet zu Keycloak weiter
4. Benutzer authentifiziert sich bei Keycloak (OIDC)
5. IAM-SCS validiert den ID-Token und erstellt eine Session

## Szenario 2: Trip erstellen

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST /trips──▶              │               │
  │               │──Route───────▶               │
  │               │              │──INSERT Trip───▶
  │               │              │◀──OK───────────│
  │◀──HTML (HTMX partial)───────│               │
```

1. Organisator erstellt einen neuen Trip über das Formular
2. Gateway routet den Request an den Trips-SCS
3. Trips-SCS validiert die Eingaben und persistiert den Trip
4. Thymeleaf rendert das HTML-Fragment, HTMX tauscht den DOM-Bereich aus

## Szenario 3: Self-Service Sign-up (S3-A02)

```
Browser        Gateway        IAM-SCS        Keycloak       PostgreSQL     RabbitMQ
  │               │              │               │              │              │
  │──GET /signup──▶              │               │              │              │
  │               │──Route───────▶               │              │              │
  │◀──Sign-up Form──────────────│               │              │              │
  │──POST /signup─▶              │               │              │              │
  │               │──Route───────▶               │              │              │
  │               │              │──Tenant.create─────────────▶│              │
  │               │              │──createUser───▶               │              │
  │               │              │◀──keycloakUserId──│          │              │
  │               │              │──Account.register──────────▶│              │
  │               │              │──TenantCreated + AccountRegistered────────▶│
  │◀──Redirect to Login─────────│               │              │              │
```

1. Benutzer oeffnet die oeffentliche Sign-up-Seite (kein Login erforderlich)
2. Formular: Reisepartei-Name, Vorname, Nachname, E-Mail, Passwort
3. SignUpService orchestriert atomar: Tenant erstellen, Keycloak-User anlegen, Account registrieren
4. Events (TenantCreated, AccountRegistered) werden via RabbitMQ publiziert
5. Trips-SCS konsumiert die Events und legt eine TravelParty-Projektion an
6. Redirect zum Gateway Login (OIDC Flow startet automatisch)

## Szenario 3b: Teilnehmer einladen und annehmen (S3-B04)

```
Browser        Gateway        Trips-SCS      PostgreSQL     RabbitMQ
  │               │              │               │              │
  │──POST invite──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──Invitation.create──────────▶│
  │◀──HTML Fragment (HTMX)──────│               │              │
  │               │              │               │              │
  │──POST accept──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──invitation.accept()         │
  │               │              │──trip.addParticipant()       │
  │               │              │──save both────▶              │
  │               │              │──ParticipantJoinedTrip──────▶│
  │◀──HTML Fragment (HTMX)──────│               │              │
```

1. Organisator laedt ein Mitglied der Reisepartei zu einem Trip ein
2. Eingeladener sieht die Einladung mit Annehmen/Ablehnen-Buttons
3. Bei Annahme: Invitation wird ACCEPTED, Participant wird zum Trip hinzugefuegt
4. ParticipantJoinedTrip-Event wird via RabbitMQ publiziert

## Szenario 3c: Trip-Einladungs-E-Mail (Iteration 4)

```
Browser        Gateway        Trips-SCS      PostgreSQL     SMTP (Mailpit)
  │               │              │               │              │
  │──POST invite──▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──Invitation.create──────────▶│
  │               │              │──InvitationCreated (enriched)│
  │               │              │──────────────────────────────▶│ (after commit)
  │◀──HTML Fragment (HTMX)──────│               │     E-Mail──▶│
```

1. Organisator laedt ein Mitglied zu einem Trip ein
2. `InvitationService` enrichiert das `InvitationCreated`-Event mit Trip-Name, Zeitraum, Einlader-Name aus Trip- und TravelParty-Aggregaten
3. Nach Transaction-Commit sendet `InvitationEmailListener` eine HTML-E-Mail via Spring Mail
4. E-Mail enthaelt Trip-Details und einen Link zur Trip-Seite

## Szenario 3d: Externe Einladung per E-Mail (Iteration 4)

```
Browser     Trips-SCS      RabbitMQ       IAM-SCS        Keycloak       SMTP
  │             │              │              │               │            │
  │──POST ──────▶              │              │               │            │
  │  external   │              │              │               │            │
  │             │──Invitation.inviteExternal()│               │            │
  │             │  [AWAITING_REGISTRATION]    │               │            │
  │             │──InvitationCreated──────────│──────────────────────────▶│ (E-Mail)
  │             │──ExternalUserInvitedToTrip──▶              │            │
  │◀──HTML──────│              │              │               │            │
  │             │              │              │               │            │
  │             │              │──consume─────▶               │            │
  │             │              │              │──createUser───▶            │
  │             │              │              │──Account.register()        │
  │             │              │              │──AccountRegistered────────▶│
  │             │              │              │               │            │
  │             │◀─consume─────│              │               │            │
  │             │──TravelParty.addMember()    │               │            │
  │             │──invitation.linkToMember()  │               │            │
  │             │──trip.addParticipant()      │               │            │
  │             │  [Auto-Accept → ACCEPTED]   │               │            │
```

1. Organisator gibt E-Mail, Name, Geburtsdatum ein und laedt eine neue Person ein
2. Trips erstellt Invitation im Status AWAITING_REGISTRATION
3. `InvitationCreated` loest E-Mail-Versand aus (Einladung mit Registrierungshinweis)
4. `ExternalUserInvitedToTrip` wird via RabbitMQ an IAM publiziert
5. IAM `ExternalInvitationConsumer` erstellt Keycloak-User und Account, publiziert `AccountRegistered`
6. Trips konsumiert `AccountRegistered`, aktualisiert TravelParty, findet wartende Einladung per E-Mail
7. Auto-Accept: Invitation → ACCEPTED, Participant wird zum Trip hinzugefuegt

## Szenario 4: Expense-Erstellung via Event-Choreografie (Iteration 5)

```
Trips-SCS      RabbitMQ       Expense-SCS      PostgreSQL
  │               │               │               │
  │──TripCreated──▶               │               │
  │               │──consume──────▶               │
  │               │               │──TripProjection.create()
  │               │               │──save──────────▶
  │               │               │               │
  │──ParticipantJoinedTrip──────▶│               │
  │               │──consume──────▶               │
  │               │               │──projection.addParticipant()
  │               │               │──save──────────▶
  │               │               │               │
  │──TripCompleted──────────────▶│               │
  │               │──consume──────▶               │
  │               │               │──Expense.create(weightings=1.0)
  │               │               │──save──────────▶
  │               │               │──ExpenseCreated (Event)
```

1. Trips publiziert `TripCreated` — Expense erstellt eine lokale `TripProjection` mit Trip-Name und TenantId
2. Bei jedem `ParticipantJoinedTrip` wird der Teilnehmer zur TripProjection hinzugefuegt
3. `TripCompleted` loest die automatische Erstellung eines `Expense`-Aggregats aus
4. Alle Teilnehmer erhalten eine Standard-Gewichtung von 1.0
5. Das `ExpenseCreated`-Event wird nach Commit publiziert

## Szenario 4b: Beleg-Erfassung und Abrechnung

```
Browser        Gateway        Expense-SCS      PostgreSQL
  │               │               │               │
  │──GET /{tripId}▶              │               │
  │               │──Route────────▶               │
  │               │               │──find Expense─▶
  │◀──HTML (Expense-Detail)──────│               │
  │               │               │               │
  │──POST receipt─▶              │               │
  │               │──Route────────▶               │
  │               │               │──addReceipt()──▶
  │◀──HTML Fragment (HTMX)──────│               │
  │               │               │               │
  │──POST settle──▶              │               │
  │               │──Route────────▶               │
  │               │               │──expense.settle()
  │               │               │──save──────────▶
  │               │               │──ExpenseSettled (Event)
  │◀──Redirect────│               │               │
```

1. Organisator oeffnet die Abrechnungsseite fuer einen abgeschlossenen Trip
2. Belege werden mit Beschreibung, Betrag, Bezahlt-von und Datum erfasst (HTMX-Partials)
3. Gewichtungen koennen pro Teilnehmer angepasst werden (Erwachsener=1.0, Teilzeit=0.5, Kind<3=0.0)
4. Saldo-Berechnung: Fuer jeden Teilnehmer wird berechnet, was er bezahlt hat minus seinen gewichteten Anteil
5. Abschluss (settle): Status wechselt zu SETTLED, `ExpenseSettled`-Event wird publiziert

## Szenario 4c: Essensplan generieren und verwalten (Iteration 7)

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST generate▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  mealplan/    │              │──find Trip─────▶
  │  generate     │              │◀──Trip─────────│
  │               │              │──MealPlan.generate(dateRange)
  │               │              │  (3 Slots/Tag: B/L/D)
  │               │              │──save MealPlan─▶
  │◀──Redirect to mealplan──────│               │
  │               │              │               │
  │──POST status──▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  mealplan/    │              │──find MealPlan─▶
  │  slots/{id}/  │              │──markSlot(SKIP|EATING_OUT)
  │  status       │              │──save──────────▶
  │◀──Redirect to mealplan──────│               │
  │               │              │               │
  │──POST recipe──▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  mealplan/    │              │──find MealPlan─▶
  │  slots/{id}/  │              │──assignRecipe(recipeId)
  │  recipe       │              │──save──────────▶
  │◀──Redirect to mealplan──────│               │
```

1. Organisator klickt "Essensplan erstellen" auf der Trip-Detailseite
2. `MealPlanService` laedt den Trip, ruft `MealPlan.generate(tenantId, tripId, dateRange)` auf
3. Factory-Methode erzeugt 3 MealSlots pro Reisetag (BREAKFAST, LUNCH, DINNER), alle im Status PLANNED
4. Essensplan-Uebersicht zeigt ein Tagesraster (Zeilen = Tage, Spalten = Mahlzeiten)
5. Jeder Slot hat ein Status-Dropdown (PLANNED → SKIP oder EATING_OUT) und eine Rezeptauswahl
6. Statusaenderung oder Rezeptzuweisung per Formular-POST, Redirect zurueck zur Uebersicht

## Szenario 4d: Einkaufsliste generieren und verwalten (Iteration 8)

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST generate▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  shopping-list│              │──find MealPlan─▶
  │  /generate    │              │◀──MealPlan─────│
  │               │              │──find Recipes──▶
  │               │              │◀──Recipes──────│
  │               │              │──find Trip─────▶
  │               │              │◀──participants─│
  │               │              │                │
  │               │              │──IngredientAggregator
  │               │              │  .aggregate(recipes, participants)
  │               │              │──ShoppingList.generate()
  │               │              │  (RECIPE items + bestehende MANUAL items)
  │               │              │──save───────────▶
  │◀──Redirect to shopping-list──│               │
  │               │              │               │
  │──GET /{tripId}▶              │               │
  │  /shopping-   │──Route───────▶               │
  │  list         │              │──find list─────▶
  │◀──HTML (hx-trigger="every 5s")──────────────│
  │               │              │               │
  │──POST status──▶              │               │
  │  /items/{id}/ │──Route───────▶               │
  │  assign       │              │──item.assign(participantId)
  │               │              │──save───────────▶
  │◀──HTML Fragment (HTMX)──────│               │
```

1. Organisator klickt "Einkaufsliste generieren" auf der Trip-Detailseite
2. `ShoppingListService` laedt MealPlan, sammelt alle Slots mit Status `PLANNED` und zugewiesenem Rezept
3. `IngredientAggregator` skaliert Zutaten nach Teilnehmerzahl (`trip.participants.size() / recipe.servings`) und aggregiert identische Zutaten (gleicher Name + Einheit)
4. `ShoppingList.generate()` erstellt RECIPE-Items aus den skalierten Zutaten, bestehende MANUAL-Items bleiben erhalten
5. Einkaufsliste wird mit HTMX-Polling alle 5 Sekunden aktualisiert (`hx-trigger="every 5s"`)
6. Status-Transitionen: OPEN -> ASSIGNED -> PURCHASED, plus Direkt-Kauf (OPEN -> PURCHASED) und Reversal (PURCHASED -> ASSIGNED)

## Szenario 4e: Unterkunft und Vorauszahlungen (Iteration 9)

```
Browser     Trips-SCS      RabbitMQ       Expense-SCS      PostgreSQL
  │             │              │               │               │
  │──POST ──────▶              │               │               │
  │  accommodation             │               │               │
  │             │──Accommodation.create()      │               │
  │             │  (name, rooms, totalPrice)   │               │
  │             │──save────────▶               │               │
  │             │──AccommodationPriceSet───────▶               │
  │◀──HTML──────│              │               │               │
  │             │              │               │               │
  │             │              │──consume───────▶               │
  │             │              │               │──tripProjection
  │             │              │               │  .setAccommodationTotalPrice()
  │             │              │               │──save──────────▶
  │             │              │               │               │
  │──POST ──────────────────────────────────────▶               │
  │  advance-   │              │               │──AdvancePaymentSuggestion
  │  payments/  │              │               │  .suggest(totalPrice, partyCount)
  │  generate   │              │               │──expense.generateAdvancePayments()
  │             │              │               │──save──────────▶
  │◀──HTML──────────────────────────────────────│               │
```

1. Organisator erstellt eine Unterkunft mit Name, Adresse, Zimmern und Gesamtpreis
2. Bei Angabe eines Preises > 0 wird `AccommodationPriceSet`-Event via RabbitMQ publiziert
3. Expense-SCS konsumiert das Event und aktualisiert `TripProjection.accommodationTotalPrice`
4. Organisator kann Vorauszahlungs-Vorschlaege generieren: `AdvancePaymentSuggestion.suggest()` rundet auf 50er-Schritte auf
5. Vorauszahlungen werden pro Reisepartei erstellt mit Bezahlt-Status (toggle)
6. Zimmerbelegung: Reiseparteien werden Zimmern zugewiesen mit Personenzahl

## Szenario 4f: Reisepartei-Abrechnung (Iteration 9)

```
Expense-SCS
  │
  │──expense.calculateBalance()
  │  (individuelle Salden pro Teilnehmer)
  │
  │──PartySettlement.aggregateByParty()
  │  (Gruppierung: participantId → partyTenantId)
  │  (Ergebnis: Saldo pro Reisepartei)
  │
  │──PartySettlement.calculateTransfers()
  │  (Greedy-Algorithmus: minimale Transfers zwischen Parteien)
  │
  │──Darstellung: "Reisepartei X zahlt Y EUR an Reisepartei Z"
```

1. Individuelle Salden werden wie bisher aus Belegen und Gewichtungen berechnet
2. `PartySettlement.aggregateByParty()` gruppiert individuelle Salden nach `partyTenantId` (aus `ParticipantJoinedTrip`-Event)
3. `PartySettlement.calculateTransfers()` berechnet minimale Ueberweisungen zwischen Reiseparteien (Greedy-Algorithmus)
4. Die UI zeigt sowohl individuelle als auch Reisepartei-Salden an

## Szenario 5: Account-Registrierung

```
Browser        Gateway        IAM-SCS        PostgreSQL     RabbitMQ
  │               │              │               │              │
  │──POST account─▶              │               │              │
  │               │──Route───────▶               │              │
  │               │              │──INSERT────────▶              │
  │               │              │◀──OK──────────│              │
  │               │              │──AccountRegistered───────────▶
  │◀──Redirect to detail────────│               │              │
```

1. Benutzer fuellt das Registrierungsformular aus (Username, E-Mail, Name, Keycloak User ID)
2. Gateway routet den Request an den IAM-SCS
3. IAM-SCS prueft Username-Eindeutigkeit und persistiert den Account
4. `AccountRegistered`-Event wird nach Commit via RabbitMQ publiziert
5. Trips-SCS konsumiert das Event und legt eine TravelParty-Projektion an

## Szenario 6: Dependent hinzufuegen (HTMX)

```
Browser                     IAM-SCS        PostgreSQL     RabbitMQ
  │                            │               │              │
  │──hx-post /dependents───────▶               │              │
  │                            │──INSERT────────▶              │
  │                            │◀──OK──────────│              │
  │                            │──DependentAddedToTenant──────▶
  │◀──HTML Fragment (HTMX)─────│               │              │
```

1. Guardian klickt auf "Mitreisenden hinzufuegen" im Account-Detail
2. HTMX sendet POST via `hx-post`, Ziel ist das Dependent-Fragment
3. IAM-SCS prueft ob Guardian existiert, erstellt Dependent
4. `DependentAddedToTenant`-Event wird via RabbitMQ publiziert
5. Thymeleaf rendert das aktualisierte Fragment, HTMX tauscht den DOM-Bereich aus

## Szenario 7: Kollaborative Reiseplanung — DatePoll und AccommodationPoll (Iteration 14)

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST ────────▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  datepoll/    │              │──DatePoll.create(tripId, dateOptions)
  │  create       │              │──save──────────▶
  │◀──Redirect to datepoll──────│               │
  │               │              │               │
  │──POST vote────▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  datepoll/    │              │──datePoll.vote(memberId, selectedOptions)
  │  vote         │              │──save──────────▶
  │◀──HTML Fragment (HTMX)──────│               │
  │               │              │               │
  │──POST confirm─▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  datepoll/    │              │──datePoll.confirm(winningOption)
  │  confirm      │              │──trip.confirmDateRange(dateRange)
  │               │              │──save both─────▶
  │◀──Redirect to trip──────────│               │
```

1. Organisator erstellt eine DatePoll mit Terminoptionen (Doodle-Stil)
2. Mitglieder stimmen ab — Mehrfachauswahl, Stimmrecht pro Account (nicht Dependent)
3. Organisator bestätigt die Gewinner-Option → `Trip.confirmDateRange()` wird aufgerufen
4. Trip-Status kann nun zu CONFIRMED wechseln

```
Browser        Gateway        Trips-SCS      PostgreSQL
  │               │              │               │
  │──POST ────────▶              │               │
  │  /{tripId}/   │──Route───────▶               │
  │  accommodation│              │──AccommodationPoll.create(tripId)
  │  poll/create  │              │──save──────────▶
  │◀──Redirect──  │              │               │
  │               │              │               │
  │──POST ────────▶              │               │
  │  candidate/   │──Route───────▶               │
  │  add          │              │──poll.addCandidate(name, url, rooms, amenities)
  │               │              │──save──────────▶
  │◀──HTML Fragment──────────────│               │
  │               │              │               │
  │──POST vote────▶              │               │
  │  /{pollId}/   │──Route───────▶               │
  │  vote         │              │──poll.vote(memberId, candidateId)
  │               │              │──save──────────▶
  │◀──HTML Fragment──────────────│               │
  │               │              │               │
  │──POST select──▶              │               │
  │  /{pollId}/   │──Route───────▶               │
  │  select       │              │──poll.selectWinner(candidateId)
  │               │              │──poll.startBooking()
  │               │              │──save──────────▶
  │◀──Redirect to poll───────────│               │
```

1. Organisator erstellt eine AccommodationPoll und fügt Kandidaten hinzu (Name, URL, Zimmer, Amenities, Adresse)
2. Mitglieder stimmen per Einzelstimme ab (Re-Vote möglich)
3. Organisator wählt den Gewinner und startet den Buchungsversuch (BookingAttempt)
4. Bei Buchungserfolg → Accommodation wird erstellt; bei Fehlschlag → Poll öffnet erneut (ADR-0022)

## Referenz

![Event Storming](../../design/evia.team.orc.thomas-klingler%20-%20Event%20Storming.jpg)
