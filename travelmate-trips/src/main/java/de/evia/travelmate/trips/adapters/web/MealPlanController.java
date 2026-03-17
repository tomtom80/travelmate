package de.evia.travelmate.trips.adapters.web;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

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
import de.evia.travelmate.trips.application.MealPlanService;
import de.evia.travelmate.trips.application.RecipeService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AssignRecipeToSlotCommand;
import de.evia.travelmate.trips.application.command.GenerateMealPlanCommand;
import de.evia.travelmate.trips.application.command.UpdateMealSlotCommand;
import de.evia.travelmate.trips.application.representation.MealPlanRepresentation;
import de.evia.travelmate.trips.application.representation.MealSlotRepresentation;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final TripService tripService;
    private final RecipeService recipeService;
    private final TravelPartyRepository travelPartyRepository;

    public MealPlanController(final MealPlanService mealPlanService,
                              final TripService tripService,
                              final RecipeService recipeService,
                              final TravelPartyRepository travelPartyRepository) {
        this.mealPlanService = mealPlanService;
        this.tripService = tripService;
        this.recipeService = recipeService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @PostMapping("/{tripId}/mealplan/generate")
    public String generate(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        mealPlanService.generateMealPlan(new GenerateMealPlanCommand(identity.tenantId().value(), tripId));
        return "redirect:/" + tripId + "/mealplan";
    }

    @GetMapping("/{tripId}/mealplan")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final MealPlanRepresentation mealPlan = mealPlanService.findByTripId(
            new TripId(tripId), identity.tenantId());
        final List<RecipeRepresentation> recipes = recipeService.findAllByTenantId(identity.tenantId());

        final var slotsByDate = mealPlan.slots().stream()
            .collect(Collectors.groupingBy(
                MealSlotRepresentation::date,
                TreeMap::new,
                Collectors.toList()
            ));

        model.addAttribute("view", "mealplan/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("mealPlan", mealPlan);
        model.addAttribute("slotsByDate", slotsByDate);
        model.addAttribute("recipes", recipes);
        return "layout/default";
    }

    @PostMapping("/{tripId}/mealplan/slots/{slotId}/status")
    public String updateSlotStatus(@AuthenticationPrincipal final Jwt jwt,
                                   @PathVariable final UUID tripId,
                                   @PathVariable final UUID slotId,
                                   @RequestParam final String status) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        try {
            MealSlotStatus.valueOf(status);
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid meal slot status: " + status);
        }
        mealPlanService.updateSlotStatus(new UpdateMealSlotCommand(tripId, slotId, status));
        return "redirect:/" + tripId + "/mealplan";
    }

    @PostMapping("/{tripId}/mealplan/slots/{slotId}/recipe")
    public String assignRecipe(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID slotId,
                               @RequestParam(required = false) final UUID recipeId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        if (recipeId != null) {
            mealPlanService.assignRecipe(new AssignRecipeToSlotCommand(tripId, slotId, recipeId));
        } else {
            mealPlanService.clearRecipe(new TripId(tripId), new MealSlotId(slotId));
        }
        return "redirect:/" + tripId + "/mealplan";
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
        if (!trip.tenantId().equals(identity.tenantId().value())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
