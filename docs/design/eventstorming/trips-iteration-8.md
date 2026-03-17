# Design-Level EventStorming: Trips SCS -- Iteration 8

**Datum**: 2026-03-17
**Scope**: Tactical DDD -- Shopping List (Einkaufsliste) + Email Notifications
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Context**: Trips (Core Subdomain)

---

## 1. Bestandsaufnahme (Ist-Zustand)

### Bestehende Aggregate und Events (Trips SCS)

```
Recipe (AggregateRoot)
  +-- RecipeId, TenantId
  +-- name: RecipeName (String, nicht leer)
  +-- servings: Servings (int > 0)
  +-- ingredients: List<Ingredient(name:String, quantity:BigDecimal, unit:String)>

MealPlan (AggregateRoot)
  +-- MealPlanId, TenantId, TripId
  +-- slots: List<MealSlot>
        +-- MealSlotId, date, MealType(BREAKFAST/LUNCH/DINNER)
        +-- status: MealSlotStatus(PLANNED/SKIP/EATING_OUT)
        +-- recipeId: UUID (nullable)

Trip (AggregateRoot)
  +-- TripId, TenantId
  +-- name, description, dateRange, organizerId, status
  +-- participants: List<Participant(participantId, firstName, lastName, stayPeriod)>

Invitation (AggregateRoot)
  +-- InvitationId, TripId
  +-- type: MEMBER | EXTERNAL
  +-- status: PENDING | ACCEPTED | DECLINED | AWAITING_REGISTRATION

TravelParty (Projection/ReadModel)
  +-- TenantId, members: List<Member>, dependents: List<TravelPartyDependent>
```

### Bestehende Event-Contracts

```
Publiziert:   TripCreated, ParticipantJoinedTrip, TripCompleted,
              InvitationCreated, ExternalUserInvitedToTrip, StayPeriodUpdated
Konsumiert:   TenantCreated, AccountRegistered, MemberAddedToTenant,
              DependentAddedToTenant, DependentRemovedFromTenant,
              MemberRemovedFromTenant, TenantDeleted
```

### Bestehende Email-Infrastruktur

```
InvitationEmailListener (adapters/mail/)
  -- Reagiert auf InvitationCreated (lokales Domain Event)
  -- Sendet HTML-Email via Spring Mail + Thymeleaf Template
  -- @Profile("!test"), @TransactionalEventListener(AFTER_COMMIT)
  -- Pattern: Event enthaelt alle Email-relevanten Daten (Name, Email, Reisename, Datum)
```

---

## 2. Zeitleiste der Domain Events (Orange Stickies)

### 2.1 Shopping List Lifecycle

```
MealPlan existiert mit zugewiesenen Recipes
  |
  [1] ShoppingListGenerated          -- Einkaufsliste wurde aus MealPlan erzeugt
  |
  [2] ManualItemAdded                -- Manueller Artikel hinzugefuegt (Snacks, Getraenke)
  |
  [3] ShoppingItemAssigned           -- Artikel einem Mitglied zugewiesen
  |
  [4] ShoppingItemUnassigned         -- Zuweisung zurueckgenommen
  |
  [5] ShoppingItemPurchased          -- Artikel als gekauft markiert
  |
  [6] ShoppingListRegenerated        -- Einkaufsliste nach MealPlan-Aenderung neu generiert
  |                                     (manuelle Artikel bleiben erhalten)
```

### 2.2 Notification Emails

```
  [7] InvitationCreated (bestehend)  -- Einladung erstellt
        --> Email an eingeladene Person (bestehend, InvitationEmailListener)
```

### Vollstaendige Zeitleiste (Shopping List)

```
                    TRIPS SCS
                    ---------
    MealPlan.assignRecipe() / updateSlotStatus()
              |
              v
    [1] GenerateShoppingList (Command)
              |
              v
    ShoppingList.generate(mealPlan, recipes, participantCount)
              |
              v
    [2] ShoppingListGenerated
              |
              +--> [3] AddManualItem --> ManualItemAdded
              |
              +--> [4] AssignShoppingItem --> ShoppingItemAssigned
              |
              +--> [5] UnassignShoppingItem --> ShoppingItemUnassigned
              |
              +--> [6] MarkItemPurchased --> ShoppingItemPurchased
              |
              +--> MealPlan aendert sich -->
                     [7] RegenerateShoppingList --> ShoppingListRegenerated
                           (RECIPE-Items neu berechnet, MANUAL-Items erhalten)
```

---

## 3. Commands (Blue Stickies)

| # | Command | Akteur | Aggregate | Vorbedingung |
|---|---------|--------|-----------|-------------|
| C1 | GenerateShoppingList | Organizer | ShoppingList | MealPlan existiert fuer Trip, noch keine ShoppingList |
| C2 | RegenerateShoppingList | Organizer | ShoppingList | ShoppingList existiert, MealPlan hat sich geaendert |
| C3 | AddManualItem | Participant | ShoppingList | ShoppingList existiert |
| C4 | RemoveManualItem | Participant | ShoppingList | Item source=MANUAL, nicht PURCHASED |
| C5 | AssignShoppingItem | Participant (self) | ShoppingList | Item status=OPEN |
| C6 | UnassignShoppingItem | Participant (self) | ShoppingList | Item status=ASSIGNED, assignedTo=currentUser |
| C7 | MarkItemPurchased | Participant (assigned) | ShoppingList | Item status=ASSIGNED, assignedTo=currentUser |

### Command-Signatur-Entwuerfe

```
GenerateShoppingList(tenantId, tripId)
  -- Laedt MealPlan + Recipes + Trip.participants intern

RegenerateShoppingList(tenantId, tripId)
  -- Loescht RECIPE-Items, berechnet neu, MANUAL-Items bleiben

AddManualItem(tenantId, tripId, itemName, quantity, unit)

RemoveManualItem(tenantId, tripId, itemId)

AssignShoppingItem(tenantId, tripId, itemId, memberId)

UnassignShoppingItem(tenantId, tripId, itemId)

MarkItemPurchased(tenantId, tripId, itemId)
```

---

## 4. Command-Event-Mapping

| Command | Aggregate | Event(s) | Invariant | Policy |
|---------|-----------|----------|-----------|--------|
| GenerateShoppingList | ShoppingList | ShoppingListGenerated | MealPlan muss existieren; noch keine ShoppingList fuer Trip | -- |
| RegenerateShoppingList | ShoppingList | ShoppingListRegenerated | ShoppingList muss existieren | RECIPE-Items entfernen, neu berechnen, MANUAL erhalten |
| AddManualItem | ShoppingList | ManualItemAdded | ShoppingList muss existieren | -- |
| RemoveManualItem | ShoppingList | (intern) | Item source=MANUAL, status != PURCHASED | -- |
| AssignShoppingItem | ShoppingList | ShoppingItemAssigned | Item status=OPEN | -- |
| UnassignShoppingItem | ShoppingList | ShoppingItemUnassigned | Item status=ASSIGNED, assignedTo=actor | -- |
| MarkItemPurchased | ShoppingList | ShoppingItemPurchased | Item status=ASSIGNED, assignedTo=actor | -- |

---

## 5. Aggregates (Yellow Stickies)

### 5.1 ShoppingList Aggregate (NEU)

**Entscheidung**: ShoppingList ist ein eigenes Aggregate, kein berechneter View. Begruendung in Abschnitt 11 (E1).

```
ShoppingList (AggregateRoot) -- NEU
  +-- ShoppingListId (VO)
  +-- TenantId (VO)
  +-- TripId (VO, aus trip package -- UUID-Wrapper)
  +-- participantCount: int
  +-- items: List<ShoppingItem>
        +-- ShoppingItemId (VO)
        +-- name: String
        +-- quantity: BigDecimal
        +-- unit: String
        +-- source: ItemSource (RECIPE | MANUAL)
        +-- status: ShoppingItemStatus (OPEN | ASSIGNED | PURCHASED)
        +-- assignedTo: UUID (nullable, memberId)
```

### 5.2 Neue Value Objects

```
ShoppingListId(UUID)                -- Aggregate-Identitaet
ShoppingItemId(UUID)                -- Entity-Identitaet innerhalb ShoppingList
ItemSource (Enum)
  RECIPE    -- Automatisch aus MealPlan-Rezepten generiert
  MANUAL    -- Manuell hinzugefuegt (Snacks, Getraenke, Haushalt)

ShoppingItemStatus (Enum)
  OPEN      -- Noch nicht zugewiesen
  ASSIGNED  -- Einem Mitglied zugewiesen
  PURCHASED -- Als gekauft markiert

ScaledIngredient (Record)           -- Hilfs-VO fuer Skalierungsberechnung
  +-- name: String
  +-- quantity: BigDecimal
  +-- unit: String
```

### 5.3 ShoppingItem (Entity innerhalb ShoppingList)

```
ShoppingItem
  +-- ShoppingItemId (VO)
  +-- name: String (nicht leer)
  +-- quantity: BigDecimal (> 0)
  +-- unit: String (nicht leer)
  +-- source: ItemSource
  +-- status: ShoppingItemStatus (default OPEN)
  +-- assignedTo: UUID (nullable)

Status-Transitionen:
  OPEN -> ASSIGNED     (assignTo)
  ASSIGNED -> OPEN     (unassign)
  ASSIGNED -> PURCHASED (markPurchased)

  PURCHASED -> ASSIGNED  (undoPurchase — versehentlicher Kauf rueckgaengig)
  OPEN -> PURCHASED      (directPurchase — Kurzweg: implizite Zuweisung + Kauf in einem Schritt)
```

---

## 6. Invarianten

### ShoppingList Aggregate

| ID | Invariante | Durchgesetzt in |
|----|-----------|-----------------|
| INV-1 | Nur eine ShoppingList pro Trip | `ShoppingList.generate()` (via Repository-Check im Service) |
| INV-2 | participantCount > 0 | `ShoppingList.generate()` |
| INV-3 | Item-Name nicht leer | `ShoppingItem` Konstruktor |
| INV-4 | Item-Quantity > 0 | `ShoppingItem` Konstruktor |
| INV-5 | Nur OPEN Items koennen assigned werden | `ShoppingList.assignItem()` |
| INV-6 | Nur ASSIGNED Items (eigene) koennen unassigned werden | `ShoppingList.unassignItem()` |
| INV-7 | ASSIGNED (eigene) oder OPEN Items koennen als purchased markiert werden | `ShoppingList.markPurchased()` / `ShoppingList.directPurchase()` |
| INV-7a | PURCHASED Items (eigene) koennen zu ASSIGNED zurueckgesetzt werden | `ShoppingList.undoPurchase()` |
| INV-8 | Nur MANUAL Items koennen entfernt werden | `ShoppingList.removeItem()` |
| INV-9 | PURCHASED Items koennen nicht entfernt werden | `ShoppingList.removeItem()` |
| INV-10 | Bei Regeneration: MANUAL Items bleiben erhalten, RECIPE Items werden neu berechnet | `ShoppingList.regenerate()` |

### Ingredient-Aggregation-Regeln

| ID | Regel | Beispiel |
|----|-------|---------|
| AGG-1 | Gleicher Name + gleiche Unit = Mengen summieren | "Tomaten 500g" + "Tomaten 300g" = "Tomaten 800g" |
| AGG-2 | Name-Matching: case-insensitive, trimmed | "tomaten" == "Tomaten" |
| AGG-3 | Unit-Matching: case-insensitive, trimmed | "g" == "G" |
| AGG-4 | Skalierung: scaledQuantity = ingredient.quantity * (participantCount / recipe.servings) | Recipe fuer 4, Trip mit 8: Menge x2 |

---

## 7. Policies / Reaktionen (Lilac Stickies)

| # | Policy | Trigger | Aktion | Typ |
|---|--------|---------|--------|-----|
| P1 | Generate-Berechnung | GenerateShoppingList | MealPlan laden, PLANNED Slots mit RecipeId filtern, Recipes laden, Ingredients skalieren und aggregieren | Application Service |
| P2 | Regeneration | RegenerateShoppingList | RECIPE-Items entfernen, P1 neu ausfuehren, MANUAL Items erhalten | ShoppingList Aggregate |
| P3 | Email bei Einladung | InvitationCreated (bestehend) | Email an Eingeladenen senden | InvitationEmailListener (bestehend) |

### Policy P1 Detail: Ingredient-Aggregation

```
1. MealPlan laden fuer Trip
2. Alle Slots filtern: status == PLANNED && recipeId != null
3. Fuer jeden Slot das Recipe laden (recipeId -> Recipe)
4. Fuer jedes Recipe:
   a. participantCount = Trip.participants.size()
   b. scaleFactor = participantCount / recipe.servings
   c. Fuer jede Ingredient:
      scaledQuantity = ingredient.quantity * scaleFactor
      --> ScaledIngredient(name, scaledQuantity, unit)
5. Alle ScaledIngredients aggregieren:
   Gruppierung nach (name.toLowerCase().trim(), unit.toLowerCase().trim())
   Mengen summieren
6. Ergebnis: Liste von ShoppingItem(source=RECIPE, status=OPEN)
```

### Policy P2 Detail: Regeneration mit Manual-Erhaltung

```
ShoppingList.regenerate(newRecipeItems):
  1. Entferne alle Items mit source == RECIPE
  2. Fuege newRecipeItems hinzu (source=RECIPE, status=OPEN)
  3. MANUAL Items bleiben unveraendert (inkl. Status ASSIGNED/PURCHASED)
```

**Entscheidung Regeneration-Trigger: Lazy (bei Seitenaufruf oder explizitem Button)**

Begruendung: Eager-Regeneration (MealPlan-Event-getrieben) wuerde erfordern, dass MealPlan Domain Events fuer jede Slot-Aenderung publiziert. Das ist aktuell nicht implementiert und wuerde den MealPlan-Aggregate unnoetig erweitern. Ein "Aktualisieren"-Button in der UI ist einfacher und gibt dem Benutzer die Kontrolle. Siehe Hot Spot HS-2.

---

## 8. Read Models (Green Stickies)

| # | Read Model | Datenquelle | Verwendet von | Neu/Bestehend |
|---|-----------|-------------|---------------|---------------|
| RM1 | ShoppingListView | ShoppingList Aggregate | Shopping List UI | NEU |
| RM2 | ShoppingListSummary | ShoppingList (aggregiert) | Trip-Detailseite (Badge/Link) | NEU |

### RM1: ShoppingListView

```
ShoppingListView
  +-- shoppingListId, tripId, tripName
  +-- participantCount: int
  +-- items: List<ShoppingItemView>
        +-- itemId, name, quantity, unit, source, status
        +-- assignedToName: String (nullable, aufgeloest via TravelParty)
  -- Gruppierung moeglich nach: source (RECIPE/MANUAL), status, oder alphabetisch
```

### RM2: ShoppingListSummary

```
ShoppingListSummary
  +-- totalItems: int
  +-- openItems: int
  +-- assignedItems: int
  +-- purchasedItems: int
```

---

## 9. External Systems (Pink Stickies)

| System | Interaktion | Richtung | Mechanismus |
|--------|-------------|----------|-------------|
| MealPlan (internes Aggregate) | Slots + RecipeIds auslesen | ShoppingListService liest | Direkte Repository-Abfrage (gleicher SCS) |
| Recipe (internes Aggregate) | Ingredients + Servings auslesen | ShoppingListService liest | Direkte Repository-Abfrage (gleicher SCS) |
| Trip (internes Aggregate) | participantCount auslesen | ShoppingListService liest | Direkte Repository-Abfrage (gleicher SCS) |
| TravelParty (Projection) | Mitglieder-Namen aufloesen | ShoppingListView Aufbereitung | Direkte Repository-Abfrage (gleicher SCS) |
| Mail Server (Mailpit) | Einladungs-Emails senden | Trips -> SMTP | Spring Mail (bestehend) |

**Anmerkung**: Alle Datenquellen fuer die ShoppingList liegen innerhalb des Trips SCS. Kein Cross-SCS-Zugriff noetig. Dies ist ein starkes Argument dafuer, ShoppingList im Trips SCS zu belassen.

---

## 10. Hot Spots (Red Stickies)

### HS-1: ShoppingList als Aggregate vs. stateless Computation (HIGH)

**Frage**: Soll die Einkaufsliste als persistiertes Aggregate modelliert werden oder als stateless Berechnung (jedes Mal aus MealPlan + Recipes berechnet)?

**Option A**: Persistiertes Aggregate `ShoppingList`
- Vorteile: Manuelle Items, Assignment, Purchase-Status, Real-Time-Polling
- Nachteile: Muss bei MealPlan-Aenderungen regeneriert werden

**Option B**: Stateless Berechnung
- Vorteile: Immer aktuell, keine Synchronisation noetig
- Nachteile: Kann keinen Zustand halten (kein Assignment, kein Purchase-Status, keine manuellen Items)

**Entscheidung**: **Option A -- Persistiertes Aggregate**.
Begruendung: Die User Stories erfordern explizit Assignment (US-052) und Purchase-Tracking (US-053), sowie manuelle Items (US-051). Dieser Zustand muss irgendwo persistiert werden. Ein Aggregate mit eigenem Lifecycle ist die natuerliche DDD-Loesung. Die Regeneration bei MealPlan-Aenderungen ist beherrschbar (Policy P2).

### HS-2: Regeneration-Trigger: Eager vs. Lazy (MEDIUM)

**Frage**: Wann wird die ShoppingList nach MealPlan-Aenderungen regeneriert?

**Option A**: Eager -- MealPlan publiziert Event bei jeder Aenderung, ShoppingList reagiert
- Vorteile: Immer aktuell
- Nachteile: MealPlan muesste Events registrieren (aktuell: keine Events), hohe Frequenz bei schnellen UI-Aenderungen

**Option B**: Lazy -- Regeneration bei explizitem Benutzer-Klick ("Aktualisieren")
- Vorteile: Einfach, keine Event-Kopplung, Benutzer entscheidet wann
- Nachteile: ShoppingList kann temporaer veraltet sein

**Option C**: Lazy-on-View -- Automatische Regeneration beim Oeffnen der Shopping-List-Seite
- Vorteile: Keine manuelle Aktion noetig, trotzdem keine Event-Kopplung
- Nachteile: Regeneration bei jedem Seitenaufruf (Performance bei grossen Listen?)

**Entscheidung**: **Option B -- Expliziter Regenerate-Button**.
Begruendung: Am einfachsten, keine MealPlan-Events noetig. Der Benutzer sieht klar, was passiert. Performance-neutral. Die UI kann einen Hinweis anzeigen ("Essensplan wurde geaendert, Einkaufsliste aktualisieren?"), aber das ist ein UI-Enhancement fuer spaeter. Fuer MVP reicht ein "Aktualisieren"-Button.

### HS-3: Ingredient-Matching-Strategie (MEDIUM)

**Frage**: Wie werden gleiche Ingredients erkannt und zusammengefasst?

**Position**: Case-insensitive String-Matching auf `(name.trim().toLowerCase(), unit.trim().toLowerCase())`. Keine Fuzzy-Matching-Logik (zu komplex fuer MVP, Error-prone).

**Bekannte Einschraenkung**: "Tomate" und "Tomaten" werden NICHT zusammengefasst. "500 g" und "0.5 kg" werden NICHT zusammengefasst. Das ist akzeptabel fuer den MVP. Die Rezept-Eingabe liegt in den Haenden des gleichen Benutzers -- konsistente Benennung ist zu erwarten.

**Entscheidung**: Einfaches case-insensitive Matching. Kein Fuzzy/NLP.

### HS-4: RECIPE-Item-Identitaet bei Regeneration (LOW)

**Frage**: Behalten RECIPE-Items ihre ShoppingItemId ueber Regenerationen hinweg?

**Position**: Nein. Bei Regeneration werden alle RECIPE-Items geloescht und neu erzeugt (mit neuen IDs). Begruendung: Die Ingredients koennen sich bei MealPlan-Aenderungen komplett aendern (anderes Rezept zugewiesen). Stabile IDs wuerden einen Diff-Algorithmus erfordern, der unverhältnismaessig komplex waere.

**Konsequenz**: Wenn ein RECIPE-Item ASSIGNED oder PURCHASED war, geht dieser Status bei Regeneration verloren. Das ist ein bewusster Trade-off: Regeneration = "Neustart" fuer Rezept-Items.

**Mitigation**: Die UI sollte eine Warnung anzeigen ("Regenerierung setzt den Status aller Rezept-Artikel zurueck").

**Entscheidung**: Keine stabile ID. RECIPE-Items werden bei Regeneration komplett ersetzt.

### HS-5: Email-Notifications: Trips-lokal vs. separater Notification-Service (MEDIUM)

**Frage**: Wo lebt die Email-Sende-Logik fuer Einladungen?

**Option A**: Bestehend beibehalten -- InvitationEmailListener im Trips SCS
- Vorteile: Bereits implementiert und funktionierend, alle Daten lokal verfuegbar
- Nachteile: Email-Verantwortung verstreut, wenn andere SCS auch Emails senden wollen

**Option B**: Neuer Notification-Service in IAM
- Vorteile: Zentralisiert Email-Versand, IAM kennt alle Email-Adressen
- Nachteile: Trips-Daten (Reisename, Datum) muessen im Event mitgeliefert werden, zusaetzliche Cross-SCS-Kopplung

**Option C**: Eigener Notification-SCS
- Vorteile: Sauberste Trennung
- Nachteile: Overengineering fuer den aktuellen Umfang

**Entscheidung**: **Option A -- Im Trips SCS beibehalten**.
Begruendung: Der bestehende InvitationEmailListener funktioniert. Die einzige Notification in Iteration 8 ist die Einladungs-Email, die bereits implementiert ist. Ein separater Service lohnt sich erst, wenn mehrere SCS Emails senden muessen (z.B. Expense-Settlement-Benachrichtigung). Bis dahin: YAGNI.

**Iteration-8-Erweiterung**: US-IAM-050 ("Email Notification for Trip Invitation") ist bereits durch den bestehenden InvitationEmailListener abgedeckt. Die Story praezisiert nur, dass bei einer Einladung einer Reisepartei ALLE Mitglieder eine Email erhalten sollen (nicht nur die direkt eingeladene Person). Das erfordert eine Erweiterung des Invitation-Flows, keine neue Architektur.

### HS-6: HTMX Polling fuer Real-Time-Updates (LOW)

**Frage**: Wie wird die Shopping List in Echtzeit aktualisiert?

**Entscheidung**: `hx-trigger="every 5s"` auf dem Shopping-List-Container. HTMX pollt den Server alle 5 Sekunden und ersetzt den HTML-Fragment. Das ist konsistent mit dem bestehenden UI-Pattern (Server-Side Rendering, kein WebSocket/SSE).

**Alternative**: SSE (Server-Sent Events) waere effizienter, erfordert aber reaktive Endpoints. Der Trips-SCS ist ein Servlet-SCS (kein WebFlux). Polling ist die einfachere Loesung.

### HS-7: Skalierungsfaktor bei variablen Teilnehmern (LOW)

**Frage**: Was ist der "participantCount" fuer die Skalierung -- alle Teilnehmer, nur Anwesende, oder gewichtet?

**Entscheidung**: `Trip.participants.size()` (alle Teilnehmer der Reise). Keine StayPeriod-basierte Differenzierung fuer den MVP. Begruendung: Die Shopping List ist eine Planungshilfe, keine exakte Kalkulation. "Lieber zu viel als zu wenig einkaufen."

---

## 11. Aggregate Design-Entscheidungen

### E1: ShoppingList als eigenes Aggregate (nicht Teil von MealPlan)

**Begruendung**:
- ShoppingList hat einen eigenen Lifecycle (kann unabhaengig von MealPlan existieren und sich aendern)
- Eigener Zustand: Assignment, Purchase-Status, manuelle Items
- Eigene Invarianten: Status-Transitionen, Manual-Erhaltung bei Regeneration
- Andere Transaktionsgrenze: MealPlan-Aenderungen betreffen ShoppingList nicht sofort
- Wuerde MealPlan-Aggregate unverhältnismaessig aufblaahen (MealPlan hat bereits Slots-Komplexitaet)

### E2: ShoppingItem als Entity innerhalb ShoppingList (nicht als eigenes Aggregate)

**Begruendung**:
- ShoppingItem hat keinen eigenstaendigen Lifecycle (existiert nur im Kontext einer ShoppingList)
- Alle Invarianten (Status-Transitionen, Assignment) beziehen sich auf den ShoppingList-Kontext
- Kein externer Zugriff auf einzelne Items ohne ShoppingList
- Konsistenz zwischen Item-Status und Regeneration muss transaktional sein

### E3: Kein separates ShoppingList-Repository-Interface in MealPlan/Recipe-Packages

**Begruendung**:
- ShoppingList ist ein eigenes Aggregate mit eigenem Package: `domain/shoppinglist/`
- ShoppingListService liest MealPlan und Recipe via deren bestehende Repositories
- Keine zirkulaere Abhaengigkeit: ShoppingList kennt MealPlan/Recipe NICHT (Service orchestriert)

---

## 12. Neue Value Objects und Enums

```java
// domain/shoppinglist/ShoppingListId.java
public record ShoppingListId(UUID value) {
    public ShoppingListId {
        argumentIsNotNull(value, "shoppingListId");
    }
}

// domain/shoppinglist/ShoppingItemId.java
public record ShoppingItemId(UUID value) {
    public ShoppingItemId {
        argumentIsNotNull(value, "shoppingItemId");
    }
}

// domain/shoppinglist/ItemSource.java
public enum ItemSource {
    RECIPE,     // Automatisch aus MealPlan-Rezepten generiert
    MANUAL      // Manuell vom Benutzer hinzugefuegt
}

// domain/shoppinglist/ShoppingItemStatus.java
public enum ShoppingItemStatus {
    OPEN,       // Noch nicht zugewiesen
    ASSIGNED,   // Einem Mitglied zugewiesen
    PURCHASED   // Gekauft
}
```

---

## 13. ShoppingList Aggregate -- Final Design

```
ShoppingList (AggregateRoot)
  +-- shoppingListId: ShoppingListId
  +-- tenantId: TenantId
  +-- tripId: TripId
  +-- participantCount: int (> 0)
  +-- items: List<ShoppingItem>

  Methoden:
    static generate(tenantId, tripId, participantCount, recipeItems) -> ShoppingList
    regenerate(newRecipeItems) -> void
      -- Entfernt alle RECIPE-Items, fuegt newRecipeItems hinzu
    addManualItem(name, quantity, unit) -> ShoppingItemId
    removeItem(itemId) -> void
      -- Nur MANUAL + nicht PURCHASED
    assignItem(itemId, memberId) -> void
      -- OPEN -> ASSIGNED
    unassignItem(itemId, memberId) -> void
      -- ASSIGNED(eigene) -> OPEN
    markPurchased(itemId, memberId) -> void
      -- ASSIGNED(eigene) -> PURCHASED

ShoppingItem (Entity innerhalb ShoppingList)
  +-- shoppingItemId: ShoppingItemId
  +-- name: String (nicht leer)
  +-- quantity: BigDecimal (> 0)
  +-- unit: String (nicht leer)
  +-- source: ItemSource
  +-- status: ShoppingItemStatus (default OPEN)
  +-- assignedTo: UUID (nullable)

  Methoden:
    assignTo(memberId) -> void
    unassign() -> void
    markPurchased() -> void
```

---

## 14. Skalierungsalgorithmus

```
Eingabe:
  - MealPlan mit Slots (nur status==PLANNED und recipeId!=null)
  - Recipes (geladen per RecipeIds aus den Slots)
  - participantCount (Trip.participants.size())

Algorithmus:
  Map<(name, unit), BigDecimal> aggregated = new HashMap<>();

  fuer jede MealSlot mit status==PLANNED und recipeId != null:
    Recipe recipe = recipes.get(slot.recipeId)
    BigDecimal scaleFactor = BigDecimal.valueOf(participantCount)
                              .divide(BigDecimal.valueOf(recipe.servings), 2, HALF_UP)

    fuer jede Ingredient in recipe.ingredients:
      String key = ingredient.name.trim().toLowerCase()
                   + "||"
                   + ingredient.unit.trim().toLowerCase()
      BigDecimal scaled = ingredient.quantity.multiply(scaleFactor)
      aggregated.merge(key, scaled, BigDecimal::add)

  fuer jeden Eintrag in aggregated:
    erstelle ShoppingItem(name=originalName, quantity=total, unit=originalUnit,
                          source=RECIPE, status=OPEN)

Beispiel:
  Recipe "Pasta Bolognese" (4 Portionen):
    - Spaghetti 500g, Hackfleisch 400g, Tomaten 800g
  Trip: 8 Teilnehmer -> scaleFactor = 8/4 = 2.0
  Ergebnis: Spaghetti 1000g, Hackfleisch 800g, Tomaten 1600g

  Zweites Rezept "Tomatensuppe" (4 Portionen):
    - Tomaten 600g, Sahne 200ml
  scaleFactor = 2.0
  Ergebnis: Tomaten 1200g, Sahne 400ml

  Aggregiert: Tomaten 2800g (1600+1200), Spaghetti 1000g, Hackfleisch 800g, Sahne 400ml
```

---

## 15. Email-Notification-Erweiterung (US-IAM-050)

### Ist-Zustand

```
InvitationService.invite() -> Invitation.create() -> registerEvent(InvitationCreated)
  -> repository.save()
  -> InvitationEmailListener.onInvitationCreated()
  -> Email an inviteeEmail
```

Die aktuelle Implementierung sendet eine Email an genau eine Person (die eingeladene). Das InvitationCreated-Event enthaelt bereits alle noetige Daten (inviteeEmail, inviteeFirstName, tripName, etc.).

### Analyse US-IAM-050

Die Story verlangt: "Wenn Organizer eine Reisepartei einlaedt, sollen ALLE Mitglieder eine Email erhalten."

**Problem**: Das aktuelle InvitationCreated-Event hat nur eine einzige `inviteeEmail`. Bei einer Einladung einer Reisepartei (Tenant mit mehreren Mitgliedern) muessten mehrere Emails gesendet werden.

**Loesungsansatz A**: InvitationCreated-Event um Liste von Email-Adressen erweitern
- Pro: Einfach
- Contra: Event-Contract-Aenderung betrifft alle Consumer

**Loesungsansatz B**: Fuer jedes Mitglied der eingeladenen Reisepartei ein separates InvitationCreated-Event registrieren
- Pro: Bestehender Listener funktioniert unveraendert
- Contra: N Events statt 1, semantisch fragwuerdig (eine Einladung, N Events?)

**Loesungsansatz C**: Neues Event `InvitationNotificationRequested` mit Liste von Empfaengern
- Pro: Saubere Trennung zwischen Einladung (Business-Event) und Notification (technisches Event)
- Contra: Zusaetzlicher Event-Typ

**Entscheidung**: **Loesungsansatz C -- Separates Notification-Event**.
Das InvitationCreated-Event bleibt ein Business-Event (1:1 pro Einladung). Ein neues lokales Event `InvitationNotificationRequested` enthaelt die Liste aller zu benachrichtigenden Email-Adressen. Der Listener reagiert auf das neue Event und sendet die Emails.

```
InvitationNotificationRequested (lokales Event, NICHT in travelmate-common)
  +-- tripName: String
  +-- tripStartDate: LocalDate
  +-- tripEndDate: LocalDate
  +-- inviterFirstName: String
  +-- inviterLastName: String
  +-- recipients: List<NotificationRecipient>
        +-- email: String
        +-- firstName: String
```

Dieses Event wird im InvitationService registriert (nicht im Aggregate), nachdem die Invitation erstellt wurde. Der Service kennt die TravelParty und kann alle Mitglieder aufloesen.

---

## 16. Zusammenfassung der Aenderungen

### Neues Aggregate
- `ShoppingList` (AggregateRoot) im Package `domain/shoppinglist/`

### Neue Value Objects / Enums
- `ShoppingListId` (Record)
- `ShoppingItemId` (Record)
- `ItemSource` (Enum: RECIPE, MANUAL)
- `ShoppingItemStatus` (Enum: OPEN, ASSIGNED, PURCHASED)

### Neue Entity
- `ShoppingItem` (Entity innerhalb ShoppingList)

### Neues Repository Interface
- `ShoppingListRepository` in `domain/shoppinglist/`

### Neue Commands
- `GenerateShoppingListCommand(tenantId, tripId)`
- `RegenerateShoppingListCommand(tenantId, tripId)`
- `AddManualItemCommand(tenantId, tripId, name, quantity, unit)`
- `RemoveManualItemCommand(tenantId, tripId, itemId)`
- `AssignShoppingItemCommand(tenantId, tripId, itemId, memberId)`
- `UnassignShoppingItemCommand(tenantId, tripId, itemId)`
- `MarkItemPurchasedCommand(tenantId, tripId, itemId, memberId)`

### Neuer Application Service
- `ShoppingListService`
  - `generate(GenerateShoppingListCommand)` -- Laedt MealPlan, Recipes, Trip; berechnet Ingredients; erstellt ShoppingList
  - `regenerate(RegenerateShoppingListCommand)` -- Neuberechnung der RECIPE-Items
  - `addManualItem(AddManualItemCommand)`
  - `removeItem(RemoveManualItemCommand)`
  - `assignItem(AssignShoppingItemCommand)`
  - `unassignItem(UnassignShoppingItemCommand)`
  - `markPurchased(MarkItemPurchasedCommand)`
  - `findByTripId(TripId, TenantId)` -- Lesen mit Name-Aufloesungen

### Neues lokales Event
- `InvitationNotificationRequested` (intern, kein RabbitMQ)

### Erweiterte Adapter
- `ShoppingListJpaRepository` in `adapters/persistence/`
- `ShoppingListController` in `adapters/web/` (mit HTMX-Polling-Endpoint)
- `InvitationNotificationListener` in `adapters/mail/` (erweitert bestehenden Listener)

### Flyway Migrations (voraussichtlich)
- V10: `shopping_list` + `shopping_item` Tabellen

### Keine neuen Event-Contracts in travelmate-common
- ShoppingList-Events sind aggregate-intern (kein anderes SCS konsumiert sie)
- Kein neuer RoutingKey noetig

### Keine Cross-SCS-Aenderungen
- Alles innerhalb des Trips SCS

---

## 17. Naechste Schritte

1. **ADR schreiben**: ShoppingList Aggregate Design (persistiert vs. berechnet)
2. **Domain-Tests (TDD Red-Green-Refactor)**:
   - ShoppingList.generate() mit Ingredient-Aggregation
   - ShoppingList.regenerate() mit Manual-Erhaltung
   - ShoppingItem Status-Transitionen
   - Skalierungsberechnung (participantCount / servings)
3. **ShoppingListService-Tests**: Orchestrierung mit MealPlan + Recipe + Trip
4. **Persistence-Adapter**: JPA-Mapping, Flyway V10
5. **Web-Adapter**: Controller mit HTMX-Polling
6. **Email-Erweiterung**: InvitationNotificationRequested Event + Listener
7. **E2E-Tests**: Shopping List CRUD + Polling + Email
