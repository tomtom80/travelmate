package de.evia.travelmate.trips.adapters.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.RecipeService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CreateRecipeCommand;
import de.evia.travelmate.trips.application.command.DeleteRecipeCommand;
import de.evia.travelmate.trips.application.command.IngredientCommand;
import de.evia.travelmate.trips.application.command.UpdateRecipeCommand;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class TripRecipeController {

    private final RecipeService recipeService;
    private final TripService tripService;
    private final TravelPartyRepository travelPartyRepository;

    public TripRecipeController(final RecipeService recipeService,
                                final TripService tripService,
                                final TravelPartyRepository travelPartyRepository) {
        this.recipeService = recipeService;
        this.tripService = tripService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/recipes")
    public String list(@AuthenticationPrincipal final Jwt jwt,
                       @PathVariable final UUID tripId,
                       final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final var recipes = recipeService.findAllByTripId(new TripId(tripId));
        final var personalRecipes = recipeService.findAllPersonalByTenantId(identity.tenantId());
        model.addAttribute("view", "recipe/trip-list");
        model.addAttribute("trip", trip);
        model.addAttribute("recipes", recipes);
        model.addAttribute("personalRecipes", personalRecipes);
        return "layout/default";
    }

    @GetMapping("/{tripId}/recipes/new")
    public String form(@AuthenticationPrincipal final Jwt jwt,
                       @PathVariable final UUID tripId,
                       final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        model.addAttribute("view", "recipe/form");
        model.addAttribute("trip", trip);
        model.addAttribute("formAction", "/" + tripId + "/recipes");
        return "layout/default";
    }

    @PostMapping("/{tripId}/recipes")
    public String create(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @RequestParam final String name,
                         @RequestParam final int servings,
                         @RequestParam("ingredientName") final List<String> ingredientNames,
                         @RequestParam("ingredientQuantity") final List<BigDecimal> ingredientQuantities,
                         @RequestParam("ingredientUnit") final List<String> ingredientUnits) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final String partyName = resolvePartyName(identity.tenantId());
        final List<IngredientCommand> ingredients = buildIngredientCommands(
            ingredientNames, ingredientQuantities, ingredientUnits);
        recipeService.createTripRecipe(
            new CreateRecipeCommand(identity.tenantId().value(), name, servings, ingredients),
            new TripId(tripId),
            partyName
        );
        return "redirect:/" + tripId + "/recipes";
    }

    @PostMapping("/{tripId}/recipes/{recipeId}/share")
    public String shareFromPersonal(@AuthenticationPrincipal final Jwt jwt,
                                    @PathVariable final UUID tripId,
                                    @PathVariable final UUID recipeId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final String partyName = resolvePartyName(identity.tenantId());
        recipeService.shareToTrip(new RecipeId(recipeId), new TripId(tripId), partyName);
        return "redirect:/" + tripId + "/recipes";
    }

    @GetMapping("/{tripId}/recipes/{recipeId}/edit")
    public String editForm(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           @PathVariable final UUID recipeId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final RecipeRepresentation recipe = recipeService.findById(new RecipeId(recipeId));
        model.addAttribute("view", "recipe/form");
        model.addAttribute("trip", trip);
        model.addAttribute("recipe", recipe);
        model.addAttribute("formAction", "/" + tripId + "/recipes/" + recipeId + "/edit");
        return "layout/default";
    }

    @PostMapping("/{tripId}/recipes/{recipeId}/edit")
    public String update(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID recipeId,
                         @RequestParam final String name,
                         @RequestParam final int servings,
                         @RequestParam("ingredientName") final List<String> ingredientNames,
                         @RequestParam("ingredientQuantity") final List<BigDecimal> ingredientQuantities,
                         @RequestParam("ingredientUnit") final List<String> ingredientUnits) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final List<IngredientCommand> ingredients = buildIngredientCommands(
            ingredientNames, ingredientQuantities, ingredientUnits);
        recipeService.updateRecipe(new UpdateRecipeCommand(recipeId, name, servings, ingredients));
        return "redirect:/" + tripId + "/recipes";
    }

    @PostMapping("/{tripId}/recipes/{recipeId}/delete")
    public String delete(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID recipeId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        recipeService.deleteRecipe(new DeleteRecipeCommand(recipeId));
        return "redirect:/" + tripId + "/recipes";
    }

    private void validateTripAccess(final UUID tripId, final ResolvedIdentity identity) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final boolean isTenantMember = trip.tenantId().equals(identity.tenantId().value());
        final boolean isParticipant = trip.participantIds().contains(identity.memberId());
        if (!isTenantMember && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private String resolvePartyName(final TenantId tenantId) {
        return travelPartyRepository.findByTenantId(tenantId)
            .map(TravelParty::name)
            .orElse("?");
    }

    private List<IngredientCommand> buildIngredientCommands(final List<String> names,
                                                            final List<BigDecimal> quantities,
                                                            final List<String> units) {
        if (names.size() != quantities.size() || names.size() != units.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ingredient fields must have matching sizes");
        }
        final List<IngredientCommand> ingredients = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i) != null && !names.get(i).isBlank()) {
                ingredients.add(new IngredientCommand(names.get(i), quantities.get(i), units.get(i)));
            }
        }
        return ingredients;
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

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
