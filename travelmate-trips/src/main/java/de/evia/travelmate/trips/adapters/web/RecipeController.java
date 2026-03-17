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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.RecipeService;
import de.evia.travelmate.trips.application.command.CreateRecipeCommand;
import de.evia.travelmate.trips.application.command.DeleteRecipeCommand;
import de.evia.travelmate.trips.application.command.IngredientCommand;
import de.evia.travelmate.trips.application.command.UpdateRecipeCommand;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final TravelPartyRepository travelPartyRepository;

    public RecipeController(final RecipeService recipeService,
                            final TravelPartyRepository travelPartyRepository) {
        this.recipeService = recipeService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final var recipes = recipeService.findAllByTenantId(identity.tenantId());
        model.addAttribute("view", "recipe/list");
        model.addAttribute("recipes", recipes);
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        requireIdentity(jwt);
        model.addAttribute("view", "recipe/form");
        model.addAttribute("formAction", "/recipes");
        return "layout/default";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal final Jwt jwt,
                         @RequestParam final String name,
                         @RequestParam final int servings,
                         @RequestParam("ingredientName") final List<String> ingredientNames,
                         @RequestParam("ingredientQuantity") final List<BigDecimal> ingredientQuantities,
                         @RequestParam("ingredientUnit") final List<String> ingredientUnits) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final List<IngredientCommand> ingredients = buildIngredientCommands(
            ingredientNames, ingredientQuantities, ingredientUnits);
        recipeService.createRecipe(new CreateRecipeCommand(
            identity.tenantId().value(), name, servings, ingredients));
        return "redirect:/recipes";
    }

    @GetMapping("/{recipeId}/edit")
    public String editForm(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID recipeId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final RecipeRepresentation recipe = recipeService.findById(new RecipeId(recipeId));
        validateRecipeAccess(recipe, identity);
        model.addAttribute("view", "recipe/form");
        model.addAttribute("recipe", recipe);
        model.addAttribute("formAction", "/recipes/" + recipeId + "/edit");
        return "layout/default";
    }

    @PostMapping("/{recipeId}/edit")
    public String update(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID recipeId,
                         @RequestParam final String name,
                         @RequestParam final int servings,
                         @RequestParam("ingredientName") final List<String> ingredientNames,
                         @RequestParam("ingredientQuantity") final List<BigDecimal> ingredientQuantities,
                         @RequestParam("ingredientUnit") final List<String> ingredientUnits) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final RecipeRepresentation existing = recipeService.findById(new RecipeId(recipeId));
        validateRecipeAccess(existing, identity);
        final List<IngredientCommand> ingredients = buildIngredientCommands(
            ingredientNames, ingredientQuantities, ingredientUnits);
        recipeService.updateRecipe(new UpdateRecipeCommand(recipeId, name, servings, ingredients));
        return "redirect:/recipes";
    }

    @PostMapping("/{recipeId}/delete")
    public String delete(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID recipeId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final RecipeRepresentation existing = recipeService.findById(new RecipeId(recipeId));
        validateRecipeAccess(existing, identity);
        recipeService.deleteRecipe(new DeleteRecipeCommand(recipeId));
        return "redirect:/recipes";
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

    private void validateRecipeAccess(final RecipeRepresentation recipe, final ResolvedIdentity identity) {
        if (!recipe.tenantId().equals(identity.tenantId().value())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Access denied");
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

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
