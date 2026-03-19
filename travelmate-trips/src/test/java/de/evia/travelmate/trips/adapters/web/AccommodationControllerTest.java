package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
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
import de.evia.travelmate.trips.application.AccommodationService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddRoomCommand;
import de.evia.travelmate.trips.application.command.AssignPartyToRoomCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomAssignmentCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomCommand;
import de.evia.travelmate.trips.application.command.SetAccommodationCommand;
import de.evia.travelmate.trips.application.representation.AccommodationRepresentation;
import de.evia.travelmate.trips.application.representation.RoomRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.ImportedRoom;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccommodationControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID ROOM_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "org@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccommodationService accommodationService;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @BeforeEach
    void setUpPartyAndTrip() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(MEMBER_UUID, MEMBER_EMAIL, "Max", "Organizer");
        when(travelPartyRepository.findByMemberEmail(MEMBER_EMAIL)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findAll()).thenReturn(List.of(party));

        final TripRepresentation tripRepr = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Test Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(tripRepr);
    }

    @Test
    void overviewShowsAccommodationPage() throws Exception {
        final AccommodationRepresentation accommodation = new AccommodationRepresentation(
            UUID.randomUUID(), TRIP_UUID, "Berghuette", "Alpweg 12", "https://booking.com",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), new BigDecimal("3000.00"),
            List.of(new RoomRepresentation(ROOM_UUID, "Zimmer 1", 2, new BigDecimal("80.00"))),
            2, List.of(), 0
        );
        when(accommodationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(accommodation));

        mockMvc.perform(get("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "accommodation/overview"))
            .andExpect(model().attributeExists("accommodation"))
            .andExpect(model().attribute("isEditable", true))
            .andExpect(model().attributeExists("partyOptions"))
            .andExpect(model().attributeExists("unassignedParties"));
    }

    @Test
    void overviewShowsEmptyStateWhenNoAccommodation() throws Exception {
        when(accommodationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.empty());

        mockMvc.perform(get("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attribute("accommodation", (Object) null));
    }

    @Test
    void setAccommodationRedirectsToOverview() throws Exception {
        when(accommodationService.setAccommodation(any(SetAccommodationCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Huette")
                .param("roomName", "Zimmer 1")
                .param("roomBedCount", "2")
                .param("roomPricePerNight", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).setAccommodation(any(SetAccommodationCommand.class));
    }

    @Test
    void addRoomRedirectsToOverview() throws Exception {
        when(accommodationService.addRoom(any(AddRoomCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Zimmer 2")
                .param("bedCount", "4"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).addRoom(any(AddRoomCommand.class));
    }

    @Test
    void updateRoomRedirectsToOverview() throws Exception {
        when(accommodationService.updateRoom(any(), any(UUID.class), any(UUID.class), anyString(), anyInt()))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms/" + ROOM_UUID + "/update")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Grosses Zimmer")
                .param("bedCount", "4"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).updateRoom(any(), any(UUID.class), any(UUID.class), anyString(), anyInt());
    }

    @Test
    void removeRoomRedirectsToOverview() throws Exception {
        when(accommodationService.removeRoom(any(RemoveRoomCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms/" + ROOM_UUID + "/delete")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).removeRoom(any(RemoveRoomCommand.class));
    }

    @Test
    void assignPartyToRoomRedirectsToOverview() throws Exception {
        when(accommodationService.assignPartyToRoom(any(AssignPartyToRoomCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms/" + ROOM_UUID + "/assign")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("partyTenantId", TENANT_UUID.toString())
                .param("partyName", "Test Party")
                .param("personCount", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).assignPartyToRoom(any(AssignPartyToRoomCommand.class));
    }

    @Test
    void removeAssignmentRedirectsToOverview() throws Exception {
        final UUID assignmentId = UUID.randomUUID();
        when(accommodationService.removeRoomAssignment(any(RemoveRoomAssignmentCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/assignments/" + assignmentId + "/delete")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).removeRoomAssignment(any(RemoveRoomAssignmentCommand.class));
    }

    @Test
    void removeAccommodationRedirectsToOverview() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/delete")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).removeAccommodation(new TripId(TRIP_UUID));
    }

    @Test
    void overviewRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final UUID otherTenantUuid = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, otherTenantUuid, "Foreign Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(get("/" + otherTenantTrip + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void setAccommodationRejectsNonOrganizer() throws Exception {
        final UUID otherMember = UUID.randomUUID();
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "PLANNING", otherMember, List.of(MEMBER_UUID, otherMember));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Huette")
                .param("roomName", "Zimmer 1")
                .param("roomBedCount", "2")
                .param("roomPricePerNight", ""))
            .andExpect(status().isForbidden());
    }

    @Test
    void setAccommodationRejectsCompletedTrip() throws Exception {
        final TripRepresentation completedTrip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "COMPLETED", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(completedTrip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Huette")
                .param("roomName", "Zimmer 1")
                .param("roomBedCount", "2")
                .param("roomPricePerNight", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void assignPartyRejectsNonOrganizer() throws Exception {
        final UUID otherMember = UUID.randomUUID();
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "PLANNING", otherMember, List.of(MEMBER_UUID, otherMember));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms/" + ROOM_UUID + "/assign")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("partyTenantId", TENANT_UUID.toString())
                .param("partyName", "Test")
                .param("personCount", "2"))
            .andExpect(status().isForbidden());
    }

    @Test
    void assignPartyRejectsCompletedTrip() throws Exception {
        final TripRepresentation completedTrip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "COMPLETED", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(completedTrip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/rooms/" + ROOM_UUID + "/assign")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("partyTenantId", TENANT_UUID.toString())
                .param("partyName", "Test")
                .param("personCount", "2"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void importAccommodationPrefillsForm() throws Exception {
        final AccommodationImportResult importResult = new AccommodationImportResult(
            "Chalet am Kogl", "Kogl 32, 8551 Wies", "https://www.huetten.com/chalet",
            null, null, new BigDecimal("4025"), "Traumhafte Huette",
            List.of(new ImportedRoom("Schlafzimmer 1", 2, null))
        );
        when(accommodationService.importFromUrl("https://www.huetten.com/chalet"))
            .thenReturn(Optional.of(importResult));

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "https://www.huetten.com/chalet"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("importSuccess", true))
            .andExpect(model().attribute("prefillName", "Chalet am Kogl"))
            .andExpect(model().attribute("prefillAddress", "Kogl 32, 8551 Wies"))
            .andExpect(model().attributeExists("prefillRooms"));
    }

    @Test
    void importAccommodationShowsErrorWhenNoData() throws Exception {
        when(accommodationService.importFromUrl("https://example.com/empty"))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "https://example.com/empty"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("importError", "accommodation.import.error"))
            .andExpect(model().attribute("prefillUrl", "https://example.com/empty"));
    }

    @Test
    void importAccommodationShowsErrorForInvalidUrl() throws Exception {
        when(accommodationService.importFromUrl("file:///etc/passwd"))
            .thenThrow(new IllegalArgumentException("Only HTTP or HTTPS URLs are allowed."));

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "file:///etc/passwd"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("importError", "accommodation.import.error.url"));
    }

    @Test
    void importAccommodationRejectsNonOrganizer() throws Exception {
        final UUID otherMember = UUID.randomUUID();
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "PLANNING", otherMember, List.of(MEMBER_UUID, otherMember));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "https://example.com"))
            .andExpect(status().isForbidden());
    }

    @Test
    void importAccommodationRejectsCompletedTrip() throws Exception {
        final TripRepresentation completedTrip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            "COMPLETED", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(completedTrip);

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "https://example.com"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void setAccommodationWithMultipleRoomsRedirectsToOverview() throws Exception {
        when(accommodationService.setAccommodation(any(SetAccommodationCommand.class)))
            .thenReturn(accommodationRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodation")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Chalet")
                .param("roomName", "Schlafzimmer 1")
                .param("roomName", "Schlafzimmer 2")
                .param("roomName", "Kinderzimmer")
                .param("roomBedCount", "2")
                .param("roomBedCount", "2")
                .param("roomBedCount", "4")
                .param("roomPricePerNight", "")
                .param("roomPricePerNight", "")
                .param("roomPricePerNight", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodation"));

        verify(accommodationService).setAccommodation(any(SetAccommodationCommand.class));
    }

    private AccommodationRepresentation accommodationRepresentation() {
        return new AccommodationRepresentation(
            UUID.randomUUID(), TRIP_UUID, "Huette", null, null,
            null, null, null,
            List.of(new RoomRepresentation(ROOM_UUID, "Zimmer 1", 2, null)),
            2, List.of(), 0
        );
    }
}
