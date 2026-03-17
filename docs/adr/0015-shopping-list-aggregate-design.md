# ADR-0015: Shopping List als persistiertes Aggregate im Trips-SCS

## Status

Accepted

## Context

Mit Iteration 8 wird eine Einkaufsliste (Shopping List) eingefuehrt, die aus dem Essensplan (MealPlan) automatisch generiert wird und zusaetzlich manuelle Eintraege erlaubt. Die zentrale Designfrage ist, ob die Einkaufsliste als eigenes persistiertes Aggregate oder als zustandslose Berechnung modelliert werden soll.

**Decision Drivers**:

1. User Stories erfordern persistenten Zustand: Zuweisung (ASSIGNED), Kauf-Status (PURCHASED), manuelle Items
2. ShoppingList hat einen eigenen Lifecycle, unabhaengig von MealPlan
3. Status-Transitionen: OPEN -> ASSIGNED -> PURCHASED, plus Ruecknahme (PURCHASED -> ASSIGNED fuer versehentliche Taps)
4. Direkt-Kauf-Shortcut: OPEN -> PURCHASED (implizite Zuweisung + Kauf in einem Schritt)
5. Regeneration bei MealPlan-Aenderungen darf manuelle Items nicht zerstoeren
6. Echtzeit-Updates via HTMX-Polling (alle 5 Sekunden)

## Decision

### 1. Persistiertes Aggregate `ShoppingList` mit `ShoppingItem`-Entities

Das `ShoppingList`-Aggregat wird pro Trip erstellt und verwaltet zwei Arten von Items:

- **RECIPE-Items**: Automatisch aus MealPlan + Rezept-Zutaten generiert, mit Skalierung nach Teilnehmerzahl
- **MANUAL-Items**: Manuell vom Benutzer hinzugefuegt (z.B. Sonnencreme, Grillkohle)

```
ShoppingList (AggregateRoot)
+-- ShoppingListId (VO)
+-- TenantId (VO)
+-- tripId: UUID
+-- items: List<ShoppingItem>
    +-- ShoppingItemId (VO)
    +-- name: String
    +-- quantity: BigDecimal
    +-- unit: String
    +-- source: RECIPE | MANUAL
    +-- status: OPEN | ASSIGNED | PURCHASED
    +-- assignedTo: UUID (nullable, participantId)
```

**Invarianten**:
- Jede ShoppingList ist an genau einen Trip gebunden (TenantId-scoped)
- Status-Transitionen: OPEN -> ASSIGNED (mit assignedTo), ASSIGNED -> PURCHASED, PURCHASED -> ASSIGNED (Reversal)
- Direkt-Kauf: OPEN -> PURCHASED (implizite Zuweisung an kaufenden Teilnehmer)
- Manuelle Items erfordern mindestens einen Namen

### 2. Status-Transitionen und Mobile-UX

```
OPEN ──────────> ASSIGNED ──────────> PURCHASED
  |                                       |
  +──── Direkt-Kauf (Shortcut) ──────────>+
                  <────── Reversal ────────+
```

- **OPEN -> ASSIGNED**: Teilnehmer uebernimmt den Einkauf fuer diesen Artikel
- **ASSIGNED -> PURCHASED**: Teilnehmer markiert Artikel als gekauft
- **PURCHASED -> ASSIGNED**: Ruecknahme fuer versehentliche Taps (Mobile-first UX im Supermarkt)
- **OPEN -> PURCHASED**: Direkt-Kauf-Shortcut mit impliziter Zuweisung an den kaufenden Teilnehmer

### 3. Regeneration (Lazy, explizit)

Die Regeneration wird durch einen expliziten "Aktualisieren"-Button ausgeloest (lazy). Es gibt keine automatische Regeneration bei MealPlan-Aenderungen.

**Regenerations-Algorithmus**:
1. Alle Items mit `source = RECIPE` entfernen (unabhaengig von ihrem Status)
2. MealPlan laden, alle Slots mit Status `PLANNED` und zugewiesenem Rezept sammeln
3. Zutaten aller Rezepte aggregieren, skalieren und als neue RECIPE-Items einfuegen
4. Items mit `source = MANUAL` bleiben vollstaendig erhalten

**Begruendung**: Kein Event-basierter Trigger, da dies eine komplexe Kopplung an MealPlan-Events erfordern wuerde. Der Benutzer behaelt die Kontrolle ueber den Zeitpunkt der Aktualisierung.

**Trade-off**: RECIPE-Items verlieren bei Regeneration ihren ASSIGNED/PURCHASED-Status. Dies ist ein bewusster Trade-off — wenn sich der Essensplan aendert, muessen Einkauefe ohnehin neu geplant werden.

### 4. Ingredient-Matching und Aggregation

Zutaten aus verschiedenen Rezepten werden zusammengefuehrt, wenn Name und Einheit uebereinstimmen:

```
Matching-Key: (name.trim().toLowerCase(), unit.trim().toLowerCase())
```

Beispiel: "Mehl 500g" + "Mehl 200g" = "Mehl 700g"

Kein Fuzzy-Matching oder NLP — einfaches String-Matching ist ausreichend fuer den aktuellen Anwendungsfall.

### 5. Skalierung nach Teilnehmerzahl

```
scaleFactor = trip.participants.size() / recipe.servings
scaledQuantity = ingredient.quantity * scaleFactor
```

Es wird die Gesamtzahl der Trip-Teilnehmer verwendet, ohne StayPeriod-basierte Differenzierung. Diese Vereinfachung ist fuer Iteration 8 ausreichend (YAGNI).

### 6. Echtzeit-Updates via HTMX-Polling

Da das Trips-SCS ein Servlet-basiertes System ist (kein WebFlux), werden Echtzeit-Updates ueber HTMX-Polling alle 5 Sekunden realisiert. Server-Sent Events (SSE) waeren zwar effizienter, erfordern aber eine reaktive Runtime.

### 7. Email-Notifications

Email-Benachrichtigungen (z.B. bei Einladungen) verbleiben im Trips-SCS und nutzen den bestehenden `InvitationEmailListener`. Kein separater Notification-Service (YAGNI).

### 8. Keine neuen Cross-SCS Events

ShoppingList-Events sind aggregate-intern und werden nicht ueber RabbitMQ publiziert. Kein neues Event-Contract in `travelmate-common` erforderlich, da kein anderes SCS die Einkaufsliste konsumiert.

## Consequences

### Positiv

- **Eigener Lifecycle**: ShoppingList ist unabhaengig von MealPlan verwaltbar
- **Klare Invarianten**: Status-Maschine im Aggregate garantiert konsistente Zustandsuebergaenge
- **Manuelle Items persistent**: Benutzer koennen beliebige Eintraege hinzufuegen, die Regeneration ueberleben
- **Mobile-first UX**: Reversal und Direkt-Kauf-Shortcut ermoeglichen schnelle Bedienung im Supermarkt
- **Einfache Regeneration**: Expliziter Button statt komplexer Event-Kopplung

### Negativ

- **RECIPE-Item-Status-Verlust**: Regeneration ersetzt alle RECIPE-Items (neue IDs), ASSIGNED/PURCHASED-Status geht verloren
- **Kein Echtzeit-Sync**: HTMX-Polling alle 5 Sekunden statt Push-basierter Updates (akzeptabler Trade-off fuer Servlet-Architektur)
- **Einfache Skalierung**: Keine StayPeriod-Differenzierung bei der Mengenskalierung (bewusste Vereinfachung)
- **Neue Flyway-Migration**: V10 fuer `shopping_list` + `shopping_item` Tabellen im Trips-SCS

## Alternatives

### Option 2: Zustandslose Berechnung (jedes Mal aus MealPlan + Recipes berechnet)

- **Vorteile**: Kein zusaetzlicher Persistenz-Aufwand, immer aktuell
- **Nachteile**: Kein Status-Tracking (ASSIGNED, PURCHASED) moeglich, keine manuellen Items, keine Zuweisung an Teilnehmer — widerspricht den User Stories grundlegend

### Option 3: ShoppingItem als Teil des MealPlan-Aggregates

- **Vorteile**: Kein neues Aggregate, automatische Konsistenz mit MealPlan
- **Nachteile**: MealPlan-Aggregate wuerde ueberladen (Single Responsibility Violation), unterschiedliche Lifecycles erzwingen unnatuerliche Kopplung, Regeneration wuerde MealPlan-Invarianten gefaehrden

## Related

- ADR-0001: SCS-Architektur
- ADR-0008: DDD + Hexagonale Architektur
- ADR-0011: Ubiquitous Language
- ADR-0014: Expense Domain Design (Vorbild fuer Aggregate-Design)
