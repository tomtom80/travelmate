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
import de.evia.travelmate.trips.application.InvitationService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID ORGANIZER_UUID = UUID.randomUUID();
    private static final UUID INVITEE_UUID = UUID.randomUUID();
    private static final String ORGANIZER_EMAIL = "org@test.de";
    private static final String INVITEE_EMAIL = "inv@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    private TravelParty party;

    @BeforeEach
    void setUpParty() {
        party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_UUID, ORGANIZER_EMAIL, "Max", "Org");
        party.addMember(INVITEE_UUID, INVITEE_EMAIL, "Lisa", "Inv");
        when(travelPartyRepository.findByMemberEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByMemberEmail(INVITEE_EMAIL)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
    }

    @Test
    void listShowsEmptyTripsWhenNoTravelParty() throws Exception {
        mockMvc.perform(get("/")
                .with(jwt().jwt(j -> j.claim("email", "unknown@test.de"))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/list"))
            .andExpect(model().attributeExists("trips"));
    }

    @Test
    void listShowsTrips() throws Exception {
        when(tripService.findAllByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of());

        mockMvc.perform(get("/")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/list"))
            .andExpect(model().attributeExists("trips"));
    }

    @Test
    void detailShowsTripWithInvitations() throws Exception {
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID, List.of(ORGANIZER_UUID)
        );

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/detail"))
            .andExpect(model().attributeExists("trip"))
            .andExpect(model().attributeExists("participants"))
            .andExpect(model().attributeExists("invitations"))
            .andExpect(model().attributeExists("invitableMembers"));
    }

    @Test
    void inviteCreatesInvitationAndReturnsFragment() throws Exception {
        final InvitationRepresentation inv = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID, "PENDING"
        );

        when(invitationService.invite(any(InviteParticipantCommand.class))).thenReturn(inv);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of(inv));

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .param("inviteeId", INVITEE_UUID.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"))
            .andExpect(model().attributeExists("invitations"));
    }

    @Test
    void acceptInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();

        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/" + invitationUuid + "/accept")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).accept(new InvitationId(invitationUuid));
    }

    @Test
    void declineInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();

        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/" + invitationUuid + "/decline")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).decline(new InvitationId(invitationUuid));
    }

    @Test
    void confirmTripRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/confirm")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(tripService).confirmTrip(new TripId(TRIP_UUID));
    }

    @Test
    void completeTripRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/complete")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().is3xxRedirection());

        verify(tripService).completeTrip(new TripId(TRIP_UUID));
    }

    @Test
    void setStayPeriodRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/participants/" + ORGANIZER_UUID + "/stay-period")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .param("arrivalDate", "2026-03-16")
                .param("departureDate", "2026-03-20"))
            .andExpect(status().is3xxRedirection());

        verify(tripService).setStayPeriod(new SetStayPeriodCommand(
            TRIP_UUID, ORGANIZER_UUID, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)));
    }
}
