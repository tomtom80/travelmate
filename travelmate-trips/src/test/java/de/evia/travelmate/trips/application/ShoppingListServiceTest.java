package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddManualShoppingItemCommand;
import de.evia.travelmate.trips.application.command.AssignShoppingItemCommand;
import de.evia.travelmate.trips.application.command.GenerateShoppingListCommand;
import de.evia.travelmate.trips.application.command.MarkShoppingItemPurchasedCommand;
import de.evia.travelmate.trips.application.command.RemoveShoppingItemCommand;
import de.evia.travelmate.trips.application.command.UndoShoppingItemPurchaseCommand;
import de.evia.travelmate.trips.application.command.UnassignShoppingItemCommand;
import de.evia.travelmate.trips.application.representation.ShoppingListRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.recipe.Servings;
import de.evia.travelmate.trips.domain.shoppinglist.ItemSource;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingList;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListRepository;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final TenantId TENANT_ID = new TenantId(TENANT_UUID);

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelPartyRepository travelPartyRepository;

    @InjectMocks
    private ShoppingListService shoppingListService;

    @Test
    void generateCreatesShoppingListFromMealPlan() {
        final Trip trip = createTrip(4);
        final TripId tripId = trip.tripId();
        final Recipe recipe = createRecipe();
        final MealPlan mealPlan = createMealPlanWithRecipe(tripId, recipe.recipeId().value());

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.empty());
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(recipeRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(recipe));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        final ShoppingListRepresentation result = shoppingListService.generate(
            new GenerateShoppingListCommand(TENANT_UUID, tripId.value()), MEMBER_ID);

        assertThat(result).isNotNull();
        assertThat(result.recipeItems()).isNotEmpty();
        assertThat(result.participantCount()).isEqualTo(4);
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void generateRejectsDuplicate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList existing = ShoppingList.generate(TENANT_ID, tripId, List.of());
        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> shoppingListService.generate(
            new GenerateShoppingListCommand(TENANT_UUID, tripId.value()), MEMBER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void generateWithNoMealPlanCreatesEmptyList() {
        final TripId tripId = new TripId(UUID.randomUUID());
        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.empty());
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(createTripWithId(tripId, 2)));

        final ShoppingListRepresentation result = shoppingListService.generate(
            new GenerateShoppingListCommand(TENANT_UUID, tripId.value()), MEMBER_ID);

        assertThat(result.recipeItems()).isEmpty();
        assertThat(result.manualItems()).isEmpty();
    }

    @Test
    void regenerateReplacesRecipeItems() {
        final Trip trip = createTrip(4);
        final TripId tripId = trip.tripId();
        final Recipe recipe = createRecipe();
        final MealPlan mealPlan = createMealPlanWithRecipe(tripId, recipe.recipeId().value());
        final ShoppingList existing = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("OldIngredient", new BigDecimal("100"), "g", ItemSource.RECIPE)
        ));
        existing.addManualItem("Bier", new BigDecimal("6"), "Flaschen");

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(existing));
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(recipeRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(recipe));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        final ShoppingListRepresentation result = shoppingListService.regenerate(
            new GenerateShoppingListCommand(TENANT_UUID, tripId.value()), MEMBER_ID);

        assertThat(result.manualItems()).hasSize(1);
        assertThat(result.manualItems().getFirst().name()).isEqualTo("Bier");
        assertThat(result.recipeItems().stream()
            .anyMatch(i -> i.name().equals("OldIngredient"))).isFalse();
    }

    @Test
    void addManualItemCreatesListIfNotExists() {
        final TripId tripId = new TripId(UUID.randomUUID());
        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.empty());
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        final var itemId = shoppingListService.addManualItem(
            new AddManualShoppingItemCommand(TENANT_UUID, tripId.value(),
                "Chips", new BigDecimal("2"), "Tueten"));

        assertThat(itemId).isNotNull();
        // save called twice: once for new list creation, once after adding item
        verify(shoppingListRepository, times(2)).save(any(ShoppingList.class));
    }

    @Test
    void assignItemDelegatesToAggregate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("Milch", new BigDecimal("2"), "l", ItemSource.RECIPE)
        ));
        final UUID itemId = list.items().getFirst().shoppingItemId().value();
        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shoppingListService.assignItem(new AssignShoppingItemCommand(
            TENANT_UUID, tripId.value(), itemId, MEMBER_ID));

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void unassignItemDelegatesToAggregate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("Milch", new BigDecimal("2"), "l", ItemSource.RECIPE)
        ));
        final UUID itemId = list.items().getFirst().shoppingItemId().value();
        list.assignItem(list.items().getFirst().shoppingItemId(), MEMBER_ID);

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shoppingListService.unassignItem(new UnassignShoppingItemCommand(
            TENANT_UUID, tripId.value(), itemId, MEMBER_ID));

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void markPurchasedDelegatesToAggregate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("Brot", new BigDecimal("1"), "Stueck", ItemSource.RECIPE)
        ));
        final UUID itemId = list.items().getFirst().shoppingItemId().value();

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shoppingListService.markPurchased(new MarkShoppingItemPurchasedCommand(
            TENANT_UUID, tripId.value(), itemId, MEMBER_ID));

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void undoPurchaseDelegatesToAggregate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("Eier", new BigDecimal("12"), "Stueck", ItemSource.RECIPE)
        ));
        final UUID itemId = list.items().getFirst().shoppingItemId().value();
        list.assignItem(list.items().getFirst().shoppingItemId(), MEMBER_ID);
        list.markPurchased(list.items().getFirst().shoppingItemId(), MEMBER_ID);

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shoppingListService.undoPurchase(new UndoShoppingItemPurchaseCommand(
            TENANT_UUID, tripId.value(), itemId));

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void removeItemDelegatesToAggregate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of());
        list.addManualItem("Chips", new BigDecimal("2"), "Tueten");
        final UUID itemId = list.items().getFirst().shoppingItemId().value();

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shoppingListService.removeItem(new RemoveShoppingItemCommand(
            TENANT_UUID, tripId.value(), itemId));

        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    void findByTripIdThrowsWhenNotFound() {
        final TripId tripId = new TripId(UUID.randomUUID());
        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.findByTripId(tripId, TENANT_ID, MEMBER_ID))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByTripIdResolvesMemberNames() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(TENANT_ID, tripId, List.of(
            new ShoppingItem("Kaese", new BigDecimal("300"), "g", ItemSource.RECIPE)
        ));
        list.assignItem(list.items().getFirst().shoppingItemId(), MEMBER_ID);

        final TravelParty party = TravelParty.create(TENANT_ID, "Mueller Familie");
        party.addMember(MEMBER_ID, "anna@test.de", "Anna", "Mueller");

        when(shoppingListRepository.findByTripIdAndTenantId(tripId, TENANT_ID))
            .thenReturn(Optional.of(list));
        when(travelPartyRepository.findByTenantId(TENANT_ID))
            .thenReturn(Optional.of(party));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(createTripWithId(tripId, 4)));

        final ShoppingListRepresentation result = shoppingListService.findByTripId(
            tripId, TENANT_ID, MEMBER_ID);

        assertThat(result.recipeItems().getFirst().assigneeName()).isEqualTo("Anna");
        assertThat(result.recipeItems().getFirst().assignedToCurrentUser()).isTrue();
    }

    private Trip createTrip(final int participantCount) {
        final List<UUID> participantIds = new java.util.ArrayList<>();
        participantIds.add(MEMBER_ID);
        for (int i = 1; i < participantCount; i++) {
            participantIds.add(UUID.randomUUID());
        }
        return Trip.plan(TENANT_ID, new TripName("Testtrip"), null,
            new DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 20)),
            MEMBER_ID, participantIds);
    }

    private Trip createTripWithId(final TripId tripId, final int participantCount) {
        final List<UUID> participantIds = new java.util.ArrayList<>();
        participantIds.add(MEMBER_ID);
        for (int i = 1; i < participantCount; i++) {
            participantIds.add(UUID.randomUUID());
        }
        return Trip.plan(TENANT_ID, new TripName("Testtrip"), null,
            new DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 20)),
            MEMBER_ID, participantIds);
    }

    private Recipe createRecipe() {
        return Recipe.create(TENANT_ID, new RecipeName("Pasta Bolognese"), new Servings(4),
            List.of(
                new Ingredient("Spaghetti", new BigDecimal("500"), "g"),
                new Ingredient("Hackfleisch", new BigDecimal("400"), "g"),
                new Ingredient("Tomaten", new BigDecimal("800"), "g")
            ));
    }

    private MealPlan createMealPlanWithRecipe(final TripId tripId, final UUID recipeId) {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, tripId,
            new DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15)));
        mealPlan.assignRecipe(mealPlan.slots().getFirst().mealSlotId(), recipeId);
        return mealPlan;
    }
}
