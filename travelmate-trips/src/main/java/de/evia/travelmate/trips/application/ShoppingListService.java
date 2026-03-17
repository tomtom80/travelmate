package de.evia.travelmate.trips.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddManualShoppingItemCommand;
import de.evia.travelmate.trips.application.command.AssignShoppingItemCommand;
import de.evia.travelmate.trips.application.command.GenerateShoppingListCommand;
import de.evia.travelmate.trips.application.command.MarkShoppingItemPurchasedCommand;
import de.evia.travelmate.trips.application.command.RemoveShoppingItemCommand;
import de.evia.travelmate.trips.application.command.UndoShoppingItemPurchaseCommand;
import de.evia.travelmate.trips.application.command.UnassignShoppingItemCommand;
import de.evia.travelmate.trips.application.representation.ShoppingItemRepresentation;
import de.evia.travelmate.trips.application.representation.ShoppingListRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.shoppinglist.IngredientAggregator;
import de.evia.travelmate.trips.domain.shoppinglist.ItemSource;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemId;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingList;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListRepository;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@Service
@Transactional
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;
    private final TripRepository tripRepository;
    private final TravelPartyRepository travelPartyRepository;

    public ShoppingListService(final ShoppingListRepository shoppingListRepository,
                               final MealPlanRepository mealPlanRepository,
                               final RecipeRepository recipeRepository,
                               final TripRepository tripRepository,
                               final TravelPartyRepository travelPartyRepository) {
        this.shoppingListRepository = shoppingListRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
        this.tripRepository = tripRepository;
        this.travelPartyRepository = travelPartyRepository;
    }

    public ShoppingListRepresentation generate(final GenerateShoppingListCommand command,
                                               final UUID currentMemberId) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());

        if (shoppingListRepository.findByTripIdAndTenantId(tripId, tenantId).isPresent()) {
            throw new IllegalStateException(
                "A shopping list already exists for trip " + command.tripId());
        }

        final List<ShoppingItem> recipeItems = computeRecipeItems(tripId, tenantId);
        final ShoppingList shoppingList = ShoppingList.generate(tenantId, tripId, recipeItems);
        shoppingListRepository.save(shoppingList);

        return toRepresentation(shoppingList, tenantId, currentMemberId);
    }

    public ShoppingListRepresentation regenerate(final GenerateShoppingListCommand command,
                                                 final UUID currentMemberId) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        final List<ShoppingItem> newRecipeItems = computeRecipeItems(tripId, tenantId);
        shoppingList.regenerate(newRecipeItems);
        shoppingListRepository.save(shoppingList);

        return toRepresentation(shoppingList, tenantId, currentMemberId);
    }

    public ShoppingItemId addManualItem(final AddManualShoppingItemCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());

        final ShoppingList shoppingList = shoppingListRepository
            .findByTripIdAndTenantId(tripId, tenantId)
            .orElseGet(() -> {
                final ShoppingList newList = ShoppingList.generate(tenantId, tripId, List.of());
                return shoppingListRepository.save(newList);
            });

        final ShoppingItemId itemId = shoppingList.addManualItem(
            command.name(), command.quantity(), command.unit());
        shoppingListRepository.save(shoppingList);
        return itemId;
    }

    public void removeItem(final RemoveShoppingItemCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        shoppingList.removeItem(new ShoppingItemId(command.itemId()));
        shoppingListRepository.save(shoppingList);
    }

    public void assignItem(final AssignShoppingItemCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        shoppingList.assignItem(new ShoppingItemId(command.itemId()), command.memberId());
        shoppingListRepository.save(shoppingList);
    }

    public void unassignItem(final UnassignShoppingItemCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        shoppingList.unassignItem(new ShoppingItemId(command.itemId()),
            command.requestingMemberId());
        shoppingListRepository.save(shoppingList);
    }

    public void markPurchased(final MarkShoppingItemPurchasedCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        shoppingList.markPurchased(new ShoppingItemId(command.itemId()),
            command.requestingMemberId());
        shoppingListRepository.save(shoppingList);
    }

    public void undoPurchase(final UndoShoppingItemPurchaseCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);

        shoppingList.undoPurchase(new ShoppingItemId(command.itemId()));
        shoppingListRepository.save(shoppingList);
    }

    @Transactional(readOnly = true)
    public ShoppingListRepresentation findByTripId(final TripId tripId,
                                                    final TenantId tenantId,
                                                    final UUID currentMemberId) {
        final ShoppingList shoppingList = findShoppingList(tripId, tenantId);
        return toRepresentation(shoppingList, tenantId, currentMemberId);
    }

    @Transactional(readOnly = true)
    public boolean existsByTripId(final TripId tripId, final TenantId tenantId) {
        return shoppingListRepository.findByTripIdAndTenantId(tripId, tenantId).isPresent();
    }

    private ShoppingList findShoppingList(final TripId tripId, final TenantId tenantId) {
        return shoppingListRepository.findByTripIdAndTenantId(tripId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("ShoppingList",
                tripId.value().toString()));
    }

    private List<ShoppingItem> computeRecipeItems(final TripId tripId, final TenantId tenantId) {
        final MealPlan mealPlan = mealPlanRepository.findByTripId(tripId).orElse(null);
        if (mealPlan == null) {
            return List.of();
        }

        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip", tripId.value().toString()));
        final int participantCount = trip.participants().size();
        if (participantCount == 0) {
            return List.of();
        }

        final List<Recipe> recipes = recipeRepository.findAllByTenantId(tenantId);
        final Map<UUID, Recipe> recipesById = recipes.stream()
            .collect(Collectors.toMap(r -> r.recipeId().value(), Function.identity()));

        return IngredientAggregator.aggregate(mealPlan, recipesById, participantCount);
    }

    private ShoppingListRepresentation toRepresentation(final ShoppingList shoppingList,
                                                         final TenantId tenantId,
                                                         final UUID currentMemberId) {
        final Map<UUID, String> memberNames = resolveMemberNames(tenantId);
        final int participantCount = tripRepository.findById(shoppingList.tripId())
            .map(trip -> trip.participants().size())
            .orElse(0);

        final List<ShoppingItemRepresentation> recipeItems = shoppingList.items().stream()
            .filter(item -> item.source() == ItemSource.RECIPE)
            .map(item -> ShoppingItemRepresentation.from(item,
                resolveName(memberNames, item.assignedTo()), currentMemberId))
            .toList();

        final List<ShoppingItemRepresentation> manualItems = shoppingList.items().stream()
            .filter(item -> item.source() == ItemSource.MANUAL)
            .map(item -> ShoppingItemRepresentation.from(item,
                resolveName(memberNames, item.assignedTo()), currentMemberId))
            .toList();

        return new ShoppingListRepresentation(
            shoppingList.shoppingListId().value(),
            shoppingList.tripId().value(),
            recipeItems,
            manualItems,
            participantCount
        );
    }

    private Map<UUID, String> resolveMemberNames(final TenantId tenantId) {
        return travelPartyRepository.findByTenantId(tenantId)
            .map(party -> party.members().stream()
                .collect(Collectors.toMap(Member::memberId, Member::firstName)))
            .orElse(Map.of());
    }

    private String resolveName(final Map<UUID, String> memberNames, final UUID memberId) {
        if (memberId == null) {
            return null;
        }
        return memberNames.getOrDefault(memberId, "Unbekannt");
    }
}
