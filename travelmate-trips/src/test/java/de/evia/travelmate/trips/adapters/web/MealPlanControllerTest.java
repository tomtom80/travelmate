package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.MealPlanService;
import de.evia.travelmate.trips.application.RecipeService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AssignRecipeToSlotCommand;
import de.evia.travelmate.trips.application.command.GenerateMealPlanCommand;
import de.evia.travelmate.trips.application.command.UpdateMealSlotCommand;
import de.evia.travelmate.trips.application.representation.MealPlanRepresentation;
import de.evia.travelmate.trips.application.representation.MealSlotRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MealPlanControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID SLOT_UUID = UUID.randomUUID();
    private static final UUID MEAL_PLAN_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "chef@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealPlanService mealPlanService;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @BeforeEach
    void setUpPartyAndTrip() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(MEMBER_UUID, MEMBER_EMAIL, "Max", "Koch");
        when(travelPartyRepository.findByMemberEmail(MEMBER_EMAIL)).thenReturn(Optional.of(party));

        final TripRepresentation tripRepr = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Test Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(tripRepr);
    }

    @Test
    void generateMealPlanRedirectsToOverview() throws Exception {
        when(mealPlanService.generateMealPlan(any(GenerateMealPlanCommand.class)))
            .thenReturn(new MealPlanRepresentation(MEAL_PLAN_UUID, TENANT_UUID, TRIP_UUID, List.of()));

        mockMvc.perform(post("/" + TRIP_UUID + "/mealplan/generate")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/mealplan"));

        verify(mealPlanService).generateMealPlan(any(GenerateMealPlanCommand.class));
    }

    @Test
    void overviewShowsMealPlanGrid() throws Exception {
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Urlaub", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID));
        final MealPlanRepresentation mealPlan = new MealPlanRepresentation(
            MEAL_PLAN_UUID, TENANT_UUID, TRIP_UUID,
            List.of(
                new MealSlotRepresentation(SLOT_UUID, LocalDate.of(2026, 7, 1), "BREAKFAST", "PLANNED", null, null),
                new MealSlotRepresentation(UUID.randomUUID(), LocalDate.of(2026, 7, 1), "LUNCH", "PLANNED", null, null),
                new MealSlotRepresentation(UUID.randomUUID(), LocalDate.of(2026, 7, 1), "DINNER", "PLANNED", null, null)
            ));

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(mealPlanService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID)))
            .thenReturn(mealPlan);
        when(recipeService.findAllByTenantId(new TenantId(TENANT_UUID))).thenReturn(List.of());

        mockMvc.perform(get("/" + TRIP_UUID + "/mealplan")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "mealplan/overview"))
            .andExpect(model().attributeExists("mealPlan"))
            .andExpect(model().attributeExists("slotsByDate"))
            .andExpect(model().attributeExists("recipes"));
    }

    @Test
    void updateSlotStatusRedirectsToOverview() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/mealplan/slots/" + SLOT_UUID + "/status")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("status", "SKIP"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/mealplan"));

        verify(mealPlanService).updateSlotStatus(any(UpdateMealSlotCommand.class));
    }

    @Test
    void assignRecipeRedirectsToOverview() throws Exception {
        final UUID recipeUuid = UUID.randomUUID();

        mockMvc.perform(post("/" + TRIP_UUID + "/mealplan/slots/" + SLOT_UUID + "/recipe")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("recipeId", recipeUuid.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/mealplan"));

        verify(mealPlanService).assignRecipe(any(AssignRecipeToSlotCommand.class));
    }

    @Test
    void clearRecipeWhenNoRecipeIdProvided() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/mealplan/slots/" + SLOT_UUID + "/recipe")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/mealplan"));

        verify(mealPlanService).clearRecipe(new TripId(TRIP_UUID), new MealSlotId(SLOT_UUID));
    }

    @Test
    void updateSlotStatusRejectsBadStatus() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/mealplan/slots/" + SLOT_UUID + "/status")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generateRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final UUID otherTenantUuid = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, otherTenantUuid, "Foreign Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(post("/" + otherTenantTrip + "/mealplan/generate")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateSlotStatusRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final UUID otherTenantUuid = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, otherTenantUuid, "Foreign Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(post("/" + otherTenantTrip + "/mealplan/slots/" + SLOT_UUID + "/status")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("status", "SKIP"))
            .andExpect(status().isForbidden());
    }
}
