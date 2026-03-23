# Iteration 7 Plan (v0.8.0) — Meal Planning & Recipe Management

## Context

Iterations 1–6 built the IAM, Trips, and Expense bounded contexts with full lifecycle management, event-driven integration, and settlement calculation. The user's top priority for the next feature is **Essensplan** (Meal Planning) — a day×meal grid per trip. This requires a **Recipe** aggregate first (the library to pick from), then the **MealPlan** aggregate (the grid of meal slots).

Both new aggregates live in the **Trips SCS** (Core Domain) since they are tightly coupled to trip date ranges and participants.

**Current state:** 464 tests, v0.7.0 released, 0.8.0-SNAPSHOT active.

---

## Key Architectural Decisions

1. **MealPlan = separate aggregate** (not embedded in Trip)
   - Trip is already substantial; meal slots have independent lifecycle
   - One MealPlan per Trip, linked by `TripId` (unique constraint)

2. **Recipe scoped per-Tenant** (reusable across trips)
   - Families build a recipe library over time
   - `MealSlot.recipeId` is a loose reference (UUID, not FK enforced at domain level)

3. **Recipe URL import deferred** to Iteration 8 (L-sized, needs HTTP client + schema.org parsing)

4. **No cross-SCS events** needed — MealPlan and Recipe are internal to Trips SCS

---

## Stories (Implementation Order)

| # | ID | Story | Size | Backlog |
|---|---|---|---|---|
| S7-A | US-TRIPS-040 | Create Recipe Manually | M | Must |
| S7-B | US-TRIPS-044 + US-TRIPS-042 | List & Edit Recipes | S+S | Must/Should |
| S7-C | US-TRIPS-043 | Delete Recipe | S | Should |
| S7-D | US-TRIPS-030 | Create Meal Plan for Trip | L | Must |
| S7-E | US-TRIPS-031+032 | Mark MealSlot SKIP / EATING_OUT | S | Must |
| S7-F | US-TRIPS-033 | Assign Recipe to MealSlot | M | Must |
| S7-G | US-TRIPS-034 | Meal Plan Overview UI | M | Must |

**Total: 7 stories (1L + 3M + 3S) covering 9 backlog items**

---

## S7-A: Create Recipe Manually (US-TRIPS-040)

**New domain package:** `domain/recipe/`

| Class | Type | Notes |
|---|---|---|
| `Recipe` | Aggregate Root | extends `AggregateRoot`, fields: recipeId, tenantId, name, servings, ingredients |
| `RecipeId` | Value Object (record) | UUID wrapper |
| `RecipeName` | Value Object (record) | non-blank, max 200 chars |
| `Servings` | Value Object (record) | int > 0 |
| `Ingredient` | Value Object (record) | name (String), quantity (BigDecimal), unit (String) |
| `RecipeRepository` | Port (interface) | save, findById, findAllByTenantId, deleteById |

**Application layer:**
- `RecipeService` with `createRecipe(CreateRecipeCommand)`
- `CreateRecipeCommand(UUID tenantId, String name, int servings, List<IngredientCommand> ingredients)`
- `RecipeRepresentation`, `IngredientRepresentation`

**Persistence:**
- Flyway `V8__recipe_aggregate.sql`: `recipe` + `recipe_ingredient` tables
- `RecipeJpaEntity`, `IngredientJpaEntity` with `@OneToMany(cascade=ALL, orphanRemoval=true)`
- `RecipeRepositoryAdapter`

**Web:**
- `RecipeController`: GET/POST `/recipes/new`
- Template: `recipe/form.html` with dynamic ingredient rows (HTMX add/remove)

**TDD sequence:** RecipeTest → RecipeServiceTest → RecipeRepositoryAdapterTest → RecipeControllerTest

---

## S7-B: List & Edit Recipes (US-TRIPS-044 + US-TRIPS-042)

- `RecipeService.findAllByTenantId()` + `updateRecipe(UpdateRecipeCommand)`
- `Recipe.update(RecipeName, Servings, List<Ingredient>)` — replaces ingredients wholesale
- Templates: `recipe/list.html`, edit reuses `recipe/form.html`
- Controller: GET `/recipes` (list), GET/POST `/recipes/{id}/edit`

---

## S7-C: Delete Recipe (US-TRIPS-043)

- `RecipeService.deleteRecipe(DeleteRecipeCommand)`
- Guard: check `MealPlanRepository.existsSlotWithRecipe(RecipeId)` — if in use, warn + require confirmation (MealSlot reverts to PLANNED without recipe)
- DB: `ON DELETE SET NULL` on `meal_slot.recipe_id` FK as safety net
- Controller: POST `/recipes/{id}/delete` with confirmation

---

## S7-D: Create Meal Plan for Trip (US-TRIPS-030)

**New domain package:** `domain/mealplan/`

| Class | Type | Notes |
|---|---|---|
| `MealPlan` | Aggregate Root | extends `AggregateRoot`, fields: mealPlanId, tenantId, tripId, slots |
| `MealPlanId` | Value Object (record) | UUID wrapper |
| `MealSlot` | Entity | mealSlotId, date, mealType, status, recipeId (nullable) |
| `MealSlotId` | Value Object (record) | UUID wrapper |
| `MealType` | Enum | BREAKFAST, LUNCH, DINNER |
| `MealSlotStatus` | Enum | PLANNED, SKIP, EATING_OUT |
| `MealPlanRepository` | Port (interface) | save, findByTripId, existsSlotWithRecipe |

**Factory method:** `MealPlan.generate(TenantId, TripId, DateRange)` → creates 3 slots per day (B/L/D)

**Application:**
- `MealPlanService.generateMealPlan(GenerateMealPlanCommand)` — loads Trip for DateRange, calls factory
- Rejects duplicate: one MealPlan per Trip

**Persistence:**
- Flyway `V9__meal_plan_aggregate.sql`: `meal_plan` + `meal_slot` tables
- `MealPlanJpaEntity`, `MealSlotJpaEntity`

**Web:** POST `/{tripId}/mealplan/generate` button on trip detail page

**TDD sequence:** MealPlanTest → MealPlanServiceTest → MealPlanRepositoryAdapterTest → MealPlanControllerTest

---

## S7-E: Mark MealSlot SKIP / EATING_OUT (US-TRIPS-031 + 032)

- `MealPlan.markSlot(MealSlotId, MealSlotStatus)` — transitions PLANNED↔SKIP, PLANNED↔EATING_OUT
- Clears `recipeId` when marking SKIP or EATING_OUT
- `UpdateMealSlotCommand(UUID tripId, UUID slotId, String status)`
- HTMX: click slot → inline dropdown → POST swap

---

## S7-F: Assign Recipe to MealSlot (US-TRIPS-033)

- `MealPlan.assignRecipe(MealSlotId, RecipeId)` — sets recipeId, status → PLANNED
- `MealPlan.clearRecipe(MealSlotId)` — removes recipe
- Service validates recipe exists via `RecipeRepository.findById()`
- `AssignRecipeToSlotCommand(UUID tripId, UUID slotId, UUID recipeId)`
- UI: recipe picker dropdown per slot (tenant's recipe library)

---

## S7-G: Meal Plan Overview UI (US-TRIPS-034)

- Grid layout: rows = days, columns = Breakfast / Lunch / Dinner
- Each cell shows: status icon + recipe name (if assigned)
- HTMX inline editing: click cell → dropdown for status/recipe
- Link from trip detail page: "Essensplan" button (visible when trip has date range)
- Template: `mealplan/overview.html`
- i18n keys: mealplan.*, mealType.BREAKFAST/LUNCH/DINNER, mealSlotStatus.*

---

## Deferred to Iteration 8+

| Story | Reason |
|---|---|
| US-TRIPS-041: Import Recipe from URL | L-sized, needs schema.org scraping |
| US-TRIPS-005/006: Edit/Delete Trip | Independent, not blocking meal planning |
| US-TRIPS-021/022: Participant Schedule + Count | Needed for meal scaling (shopping list iteration) |
| US-INFRA-001: GitHub Actions CI | Infrastructure, independent |
| US-EXP-013/033: Advance Payment, PDF Export | Expense extras, lower priority |

---

## Cross-Cutting Concerns

- **ArchUnit:** extend `ArchitectureTest` to cover `domain/recipe/` and `domain/mealplan/`
- **Tenant isolation tests:** verify cross-tenant recipe and meal plan data is invisible
- **i18n:** add message keys in all 3 `.properties` files (DE, EN, default)
- **Navigation:** add "Rezepte" link in nav bar for recipe library

---

## Verification

```bash
# Run all Trips tests (should cover all new domain, service, persistence, controller tests)
./mvnw -pl travelmate-trips clean test

# Full build (all modules)
./mvnw clean verify

# Manual verification: start infrastructure + app, create trip, generate meal plan, assign recipe
```

---

## Critical Reference Files

- `trips/domain/trip/Trip.java` — Aggregate Root pattern (AggregateRoot extension, factory methods)
- `trips/domain/trip/DateRange.java` — Used by MealPlan.generate() to create slots
- `trips/domain/trip/Participant.java` — Entity pattern (mutable fields, no framework deps)
- `trips/adapters/persistence/TripRepositoryAdapter.java` — JPA mapping pattern
- `trips/adapters/web/TripController.java` — Controller pattern (JWT identity, HTMX)
- `trips/src/main/resources/db/migration/V3__trip_aggregate.sql` — Flyway naming convention
