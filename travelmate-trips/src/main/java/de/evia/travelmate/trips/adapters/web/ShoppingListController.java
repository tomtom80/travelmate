package de.evia.travelmate.trips.adapters.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.ShoppingListService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddManualShoppingItemCommand;
import de.evia.travelmate.trips.application.command.AssignShoppingItemCommand;
import de.evia.travelmate.trips.application.command.GenerateShoppingListCommand;
import de.evia.travelmate.trips.application.command.MarkShoppingItemPurchasedCommand;
import de.evia.travelmate.trips.application.command.RemoveShoppingItemCommand;
import de.evia.travelmate.trips.application.command.UndoShoppingItemPurchaseCommand;
import de.evia.travelmate.trips.application.command.UnassignShoppingItemCommand;
import de.evia.travelmate.trips.application.representation.ShoppingItemRepresentation;
import de.evia.travelmate.trips.application.representation.ShoppingListRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final TripService tripService;
    private final TravelPartyRepository travelPartyRepository;

    public ShoppingListController(final ShoppingListService shoppingListService,
                                  final TripService tripService,
                                  final TravelPartyRepository travelPartyRepository) {
        this.shoppingListService = shoppingListService;
        this.tripService = tripService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/shoppinglist")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           @RequestParam(defaultValue = "all") final String filter,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final boolean hasShoppingList = shoppingListService.existsByTripId(
            new TripId(tripId), identity.tenantId());

        model.addAttribute("view", "shoppinglist/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("hasShoppingList", hasShoppingList);
        model.addAttribute("filter", filter);

        if (hasShoppingList) {
            final ShoppingListRepresentation shoppingList = shoppingListService.findByTripId(
                new TripId(tripId), identity.tenantId(), identity.memberId());
            populateFilteredItems(model, shoppingList, filter);
        }

        return "layout/default";
    }

    @PostMapping("/{tripId}/shoppinglist/generate")
    public String generate(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.generate(
            new GenerateShoppingListCommand(identity.tenantId().value(), tripId),
            identity.memberId());
        return "redirect:/" + tripId + "/shoppinglist";
    }

    @PostMapping("/{tripId}/shoppinglist/regenerate")
    public String regenerate(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.regenerate(
            new GenerateShoppingListCommand(identity.tenantId().value(), tripId),
            identity.memberId());
        return "redirect:/" + tripId + "/shoppinglist";
    }

    @PostMapping("/{tripId}/shoppinglist/items")
    public String addItem(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId,
                          @RequestParam final String name,
                          @RequestParam final BigDecimal quantity,
                          @RequestParam final String unit,
                          final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.addManualItem(new AddManualShoppingItemCommand(
            identity.tenantId().value(), tripId, name, quantity, unit));
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final ShoppingListRepresentation shoppingList = shoppingListService.findByTripId(
            new TripId(tripId), identity.tenantId(), identity.memberId());
        model.addAttribute("trip", trip);
        populateFilteredItems(model, shoppingList, "all");
        return "shoppinglist/overview :: itemLists";
    }

    @PostMapping("/{tripId}/shoppinglist/items/{itemId}/assign")
    public String assignItem(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             @PathVariable final UUID itemId,
                             final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.assignItem(new AssignShoppingItemCommand(
            identity.tenantId().value(), tripId, itemId, identity.memberId()));
        return populateItemRow(tripId, itemId, identity, model);
    }

    @PostMapping("/{tripId}/shoppinglist/items/{itemId}/unassign")
    public String unassignItem(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID itemId,
                               final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.unassignItem(new UnassignShoppingItemCommand(
            identity.tenantId().value(), tripId, itemId, identity.memberId()));
        return populateItemRow(tripId, itemId, identity, model);
    }

    @PostMapping("/{tripId}/shoppinglist/items/{itemId}/purchase")
    public String purchaseItem(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID itemId,
                               final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.markPurchased(new MarkShoppingItemPurchasedCommand(
            identity.tenantId().value(), tripId, itemId, identity.memberId()));
        return populateItemRow(tripId, itemId, identity, model);
    }

    @PostMapping("/{tripId}/shoppinglist/items/{itemId}/undo-purchase")
    public String undoPurchase(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID itemId,
                               final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.undoPurchase(new UndoShoppingItemPurchaseCommand(
            identity.tenantId().value(), tripId, itemId));
        return populateItemRow(tripId, itemId, identity, model);
    }

    @DeleteMapping("/{tripId}/shoppinglist/items/{itemId}")
    public String removeItem(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             @PathVariable final UUID itemId,
                             final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        shoppingListService.removeItem(new RemoveShoppingItemCommand(
            identity.tenantId().value(), tripId, itemId));
        return "shoppinglist/overview :: emptyRow";
    }

    @GetMapping("/{tripId}/shoppinglist/items")
    public String pollItems(@AuthenticationPrincipal final Jwt jwt,
                            @PathVariable final UUID tripId,
                            @RequestParam(defaultValue = "all") final String filter,
                            final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final ShoppingListRepresentation shoppingList = shoppingListService.findByTripId(
            new TripId(tripId), identity.tenantId(), identity.memberId());
        model.addAttribute("trip", trip);
        populateFilteredItems(model, shoppingList, filter);
        return "shoppinglist/overview :: itemLists";
    }

    private String populateItemRow(final UUID tripId, final UUID itemId,
                                   final ResolvedIdentity identity, final Model model) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final ShoppingListRepresentation shoppingList = shoppingListService.findByTripId(
            new TripId(tripId), identity.tenantId(), identity.memberId());
        final ShoppingItemRepresentation item = findItem(shoppingList, itemId);
        model.addAttribute("trip", trip);
        model.addAttribute("item", item);
        return "shoppinglist/overview :: itemRow";
    }

    private ShoppingItemRepresentation findItem(final ShoppingListRepresentation shoppingList,
                                                 final UUID itemId) {
        return shoppingList.recipeItems().stream()
            .filter(i -> i.itemId().equals(itemId))
            .findFirst()
            .or(() -> shoppingList.manualItems().stream()
                .filter(i -> i.itemId().equals(itemId))
                .findFirst())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private void populateFilteredItems(final Model model,
                                       final ShoppingListRepresentation shoppingList,
                                       final String filter) {
        final List<ShoppingItemRepresentation> recipeItems = filterItems(shoppingList.recipeItems(), filter);
        final List<ShoppingItemRepresentation> manualItems = filterItems(shoppingList.manualItems(), filter);
        model.addAttribute("shoppingList", shoppingList);
        model.addAttribute("recipeItems", recipeItems);
        model.addAttribute("manualItems", manualItems);
    }

    private List<ShoppingItemRepresentation> filterItems(final List<ShoppingItemRepresentation> items,
                                                          final String filter) {
        if ("all".equals(filter)) {
            return items;
        }
        try {
            final ShoppingItemStatus status = ShoppingItemStatus.valueOf(filter.toUpperCase());
            return items.stream()
                .filter(item -> item.status() == status)
                .toList();
        } catch (final IllegalArgumentException e) {
            return items;
        }
    }

    private Optional<ResolvedIdentity> resolveIdentity(final Jwt jwt) {
        final String email = jwt.getClaimAsString("email");
        if (email == null) {
            return Optional.empty();
        }
        return travelPartyRepository.findByMemberEmail(email)
            .flatMap(party -> party.members().stream()
                .filter(m -> email.equals(m.email()))
                .findFirst()
                .map(m -> new ResolvedIdentity(party.tenantId(), m.memberId())));
    }

    private ResolvedIdentity requireIdentity(final Jwt jwt) {
        return resolveIdentity(jwt)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No travel party found for user"));
    }

    private void validateTripAccess(final UUID tripId, final ResolvedIdentity identity) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final boolean isTenantMember = trip.tenantId().equals(identity.tenantId().value());
        final boolean isParticipant = trip.participantIds().contains(identity.memberId());
        if (!isTenantMember && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
