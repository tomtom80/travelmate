package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @Test
    void listShowsTrips() throws Exception {
        when(tripService.findAllByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of());

        mockMvc.perform(get("/trips").param("tenantId", TENANT_UUID.toString()))
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
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_UUID, "org@test.de", "Max", "Org");
        party.addMember(INVITEE_UUID, "inv@test.de", "Lisa", "Inv");

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(get("/trips/" + TRIP_UUID)
                .param("tenantId", TENANT_UUID.toString())
                .param("currentMemberId", ORGANIZER_UUID.toString()))
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
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_UUID, "org@test.de", "Max", "Org");
        final InvitationRepresentation inv = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID, "PENDING"
        );

        when(invitationService.invite(any(InviteParticipantCommand.class))).thenReturn(inv);
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of(inv));

        mockMvc.perform(post("/trips/" + TRIP_UUID + "/invitations")
                .param("tenantId", TENANT_UUID.toString())
                .param("inviteeId", INVITEE_UUID.toString())
                .param("invitedBy", ORGANIZER_UUID.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"))
            .andExpect(model().attributeExists("invitations"));
    }

    @Test
    void acceptInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(INVITEE_UUID, "inv@test.de", "Lisa", "Inv");

        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/trips/" + TRIP_UUID + "/invitations/" + invitationUuid + "/accept")
                .param("tenantId", TENANT_UUID.toString())
                .param("currentMemberId", INVITEE_UUID.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).accept(new InvitationId(invitationUuid));
    }

    @Test
    void declineInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(INVITEE_UUID, "inv@test.de", "Lisa", "Inv");

        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/trips/" + TRIP_UUID + "/invitations/" + invitationUuid + "/decline")
                .param("tenantId", TENANT_UUID.toString())
                .param("currentMemberId", INVITEE_UUID.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).decline(new InvitationId(invitationUuid));
    }
}
