package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import de.evia.travelmate.trips.application.ShoppingListService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddManualShoppingItemCommand;
import de.evia.travelmate.trips.application.command.AssignShoppingItemCommand;
import de.evia.travelmate.trips.application.command.GenerateShoppingListCommand;
import de.evia.travelmate.trips.application.command.RemoveShoppingItemCommand;
import de.evia.travelmate.trips.application.command.UnassignShoppingItemCommand;
import de.evia.travelmate.trips.application.representation.ShoppingListRepresentation;
import de.evia.travelmate.trips.application.representation.ShoppingItemRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemId;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShoppingListControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID SHOPPING_LIST_UUID = UUID.randomUUID();
    private static final UUID ITEM_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "shopper@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingListService shoppingListService;

    @MockitoBean
    private MealPlanService mealPlanService;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @BeforeEach
    void setUpPartyAndTrip() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(MEMBER_UUID, MEMBER_EMAIL, "Max", "Shopper");
        when(travelPartyRepository.findByMemberEmail(MEMBER_EMAIL)).thenReturn(Optional.of(party));

        final TripRepresentation tripRepr = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Beach Trip", null,
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(tripRepr);
    }

    @Test
    void overviewReturnsPageWhenShoppingListExists() throws Exception {
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(), List.of(), 3);
        when(shoppingListService.existsByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID)))
            .thenReturn(true);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(get("/" + TRIP_UUID + "/shoppinglist")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "shoppinglist/overview"))
            .andExpect(model().attributeExists("shoppingList"))
            .andExpect(model().attributeExists("trip"));
    }

    @Test
    void overviewReturnsPageWhenNoShoppingList() throws Exception {
        when(shoppingListService.existsByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID)))
            .thenReturn(false);

        mockMvc.perform(get("/" + TRIP_UUID + "/shoppinglist")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "shoppinglist/overview"))
            .andExpect(model().attributeExists("trip"));
    }

    @Test
    void generateCreatesListAndRedirects() throws Exception {
        when(shoppingListService.generate(any(GenerateShoppingListCommand.class), eq(MEMBER_UUID)))
            .thenReturn(new ShoppingListRepresentation(
                SHOPPING_LIST_UUID, TRIP_UUID, List.of(), List.of(), 3));

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/generate")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/shoppinglist"));

        verify(shoppingListService).generate(any(GenerateShoppingListCommand.class), eq(MEMBER_UUID));
    }

    @Test
    void regenerateUpdatesListAndRedirects() throws Exception {
        when(shoppingListService.regenerate(any(GenerateShoppingListCommand.class), eq(MEMBER_UUID)))
            .thenReturn(new ShoppingListRepresentation(
                SHOPPING_LIST_UUID, TRIP_UUID, List.of(), List.of(), 3));

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/regenerate")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/shoppinglist"));

        verify(shoppingListService).regenerate(any(GenerateShoppingListCommand.class), eq(MEMBER_UUID));
    }

    @Test
    void addManualItemReturnsFragment() throws Exception {
        final ShoppingItemId newItemId = new ShoppingItemId(UUID.randomUUID());
        when(shoppingListService.addManualItem(any(AddManualShoppingItemCommand.class)))
            .thenReturn(newItemId);
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(), List.of(), 3);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/items")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Milk")
                .param("quantity", "2")
                .param("unit", "L"))
            .andExpect(status().isOk());

        verify(shoppingListService).addManualItem(any(AddManualShoppingItemCommand.class));
    }

    @Test
    void assignItemReturnsUpdatedRow() throws Exception {
        final ShoppingItemRepresentation item = new ShoppingItemRepresentation(
            ITEM_UUID, "Milk", "2", "L", "RECIPE", ShoppingItemStatus.ASSIGNED, "Max", true, false);
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(item), List.of(), 3);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/items/" + ITEM_UUID + "/assign")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk());

        verify(shoppingListService).assignItem(any(AssignShoppingItemCommand.class));
    }

    @Test
    void unassignItemReturnsUpdatedRow() throws Exception {
        final ShoppingItemRepresentation item = new ShoppingItemRepresentation(
            ITEM_UUID, "Milk", "2", "L", "RECIPE", ShoppingItemStatus.OPEN, null, false, false);
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(item), List.of(), 3);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/items/" + ITEM_UUID + "/unassign")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk());

        verify(shoppingListService).unassignItem(any(UnassignShoppingItemCommand.class));
    }

    @Test
    void purchaseItemReturnsUpdatedRow() throws Exception {
        final ShoppingItemRepresentation item = new ShoppingItemRepresentation(
            ITEM_UUID, "Milk", "2", "L", "RECIPE", ShoppingItemStatus.PURCHASED, "Max", true, false);
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(item), List.of(), 3);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(post("/" + TRIP_UUID + "/shoppinglist/items/" + ITEM_UUID + "/purchase")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk());

        verify(shoppingListService).markPurchased(any());
    }

    @Test
    void removeItemReturns200() throws Exception {
        mockMvc.perform(delete("/" + TRIP_UUID + "/shoppinglist/items/" + ITEM_UUID)
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk());

        verify(shoppingListService).removeItem(any(RemoveShoppingItemCommand.class));
    }

    @Test
    void pollingFragmentReturns200() throws Exception {
        final ShoppingListRepresentation list = new ShoppingListRepresentation(
            SHOPPING_LIST_UUID, TRIP_UUID, List.of(), List.of(), 3);
        when(shoppingListService.existsByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID)))
            .thenReturn(true);
        when(shoppingListService.findByTripId(new TripId(TRIP_UUID), new TenantId(TENANT_UUID), MEMBER_UUID))
            .thenReturn(list);

        mockMvc.perform(get("/" + TRIP_UUID + "/shoppinglist/items")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk());
    }

    @Test
    void crossTenantAccessReturnsForbidden() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final UUID otherTenantUuid = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, otherTenantUuid, "Foreign Trip", null,
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(get("/" + otherTenantTrip + "/shoppinglist")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }
}
