package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
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

import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.AccommodationService;
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.InvitationService;
import de.evia.travelmate.trips.application.MealPlanService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddParticipantToTripCommand;
import de.evia.travelmate.trips.application.command.EditTripCommand;
import de.evia.travelmate.trips.application.command.GrantTripOrganizerCommand;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.command.RemoveParticipantFromTripCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.application.representation.PartyParticipantOption;
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
    private static final UUID DEPENDENT_UUID = UUID.randomUUID();
    private static final UUID OTHER_PARTY_TENANT_UUID = UUID.randomUUID();
    private static final UUID OTHER_PARTY_MEMBER_UUID = UUID.randomUUID();
    private static final String OTHER_PARTY_MEMBER_EMAIL = "other@test.de";
    private static final String ORGANIZER_EMAIL = "org@test.de";
    private static final String INVITEE_EMAIL = "inv@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private MealPlanService mealPlanService;

    @MockitoBean
    private AccommodationService accommodationService;

    @MockitoBean
    private DatePollService datePollService;

    @MockitoBean
    private AccommodationPollService accommodationPollService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    private TravelParty party;
    private TravelParty otherParty;

    @BeforeEach
    void setUpParty() {
        party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_UUID, ORGANIZER_EMAIL, "Max", "Org");
        party.addMember(INVITEE_UUID, INVITEE_EMAIL, "Lisa", "Inv");
        party.addDependent(DEPENDENT_UUID, ORGANIZER_UUID, "Tim", "Org");
        otherParty = TravelParty.create(new TenantId(OTHER_PARTY_TENANT_UUID), "Familie Anders");
        otherParty.addMember(OTHER_PARTY_MEMBER_UUID, OTHER_PARTY_MEMBER_EMAIL, "Ola", "Anders");
        when(travelPartyRepository.findByMemberEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByMemberEmail(INVITEE_EMAIL)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByMemberId(ORGANIZER_UUID)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByMemberId(INVITEE_UUID)).thenReturn(Optional.of(party));
        when(travelPartyRepository.findByMemberId(OTHER_PARTY_MEMBER_UUID)).thenReturn(Optional.of(otherParty));
        when(travelPartyRepository.findAll()).thenReturn(List.of(party, otherParty));
    }

    @Test
    void listShowsEmptyTripsWhenNoTravelParty() throws Exception {
        mockMvc.perform(get("/")
                .with(jwt().jwt(j -> j.claim("email", "unknown@test.de"))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/list"))
            .andExpect(model().attributeExists("trips"))
            .andExpect(model().attributeExists("pendingInvitations"));
    }

    @Test
    void listShowsTrips() throws Exception {
        when(tripService.findAllByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of());
        when(invitationService.findPendingByInviteeId(ORGANIZER_UUID))
            .thenReturn(List.of());

        mockMvc.perform(get("/")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/list"))
            .andExpect(model().attributeExists("trips"))
            .andExpect(model().attributeExists("pendingInvitations"));
    }

    @Test
    void listShowsPendingInvitations() throws Exception {
        final InvitationRepresentation pendingInv = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, INVITEE_UUID, OTHER_PARTY_MEMBER_UUID,
            null, TENANT_UUID, "MEMBER", "PENDING"
        );
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID, List.of(ORGANIZER_UUID)
        );

        when(tripService.findAllByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of(trip));
        when(invitationService.findPendingByInviteeId(INVITEE_UUID))
            .thenReturn(List.of(pendingInv));
        when(tripService.findById(new TripId(TRIP_UUID)))
            .thenReturn(trip);

        mockMvc.perform(get("/")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("pendingInvitations"))
            .andDo(result -> {
                @SuppressWarnings("unchecked")
                final List<de.evia.travelmate.trips.application.representation.PendingInvitationView> invitations =
                    (List<de.evia.travelmate.trips.application.representation.PendingInvitationView>)
                        result.getModelAndView().getModel().get("pendingInvitations");
                assertThat(invitations).singleElement()
                    .extracting(de.evia.travelmate.trips.application.representation.PendingInvitationView::inviterName)
                    .isEqualTo("Ola Anders");
            });
    }

    @Test
    void detailShowsTripWithInvitations() throws Exception {
        final InvitationRepresentation invitation = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, OTHER_PARTY_MEMBER_UUID, ORGANIZER_UUID,
            OTHER_PARTY_MEMBER_EMAIL, OTHER_PARTY_TENANT_UUID, "MEMBER", "PENDING"
        );
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID, List.of(ORGANIZER_UUID)
        );

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of(invitation));

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/detail"))
            .andExpect(model().attributeExists("trip"))
            .andExpect(model().attributeExists("participants"))
            .andExpect(model().attributeExists("invitations"))
            .andExpect(model().attributeExists("invitableMembers"))
            .andDo(result -> {
                @SuppressWarnings("unchecked")
                final List<de.evia.travelmate.trips.application.representation.InvitationView> invitations =
                    (List<de.evia.travelmate.trips.application.representation.InvitationView>)
                        result.getModelAndView().getModel().get("invitations");
                assertThat(invitations).singleElement().satisfies(inv -> {
                    assertThat(inv.inviteeName()).isEqualTo("Ola Anders");
                    assertThat(inv.targetPartyName()).isEqualTo("Familie Anders");
                });
            });
    }

    @Test
    void detailReflectsRenamedTargetParty() throws Exception {
        otherParty.updateName("Familie Anders Neu");
        final InvitationRepresentation invitation = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, OTHER_PARTY_MEMBER_UUID, ORGANIZER_UUID,
            OTHER_PARTY_MEMBER_EMAIL, OTHER_PARTY_TENANT_UUID, "MEMBER", "PENDING"
        );
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID, List.of(ORGANIZER_UUID)
        );

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of(invitation));

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andDo(result -> {
                @SuppressWarnings("unchecked")
                final List<de.evia.travelmate.trips.application.representation.InvitationView> invitations =
                    (List<de.evia.travelmate.trips.application.representation.InvitationView>)
                        result.getModelAndView().getModel().get("invitations");
                assertThat(invitations).singleElement()
                    .extracting(de.evia.travelmate.trips.application.representation.InvitationView::targetPartyName)
                    .isEqualTo("Familie Anders Neu");
            });
    }

    @Test
    void invitationLandingRedirectsInviteeToTripList() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();
        final InvitationRepresentation invitation = new InvitationRepresentation(
            invitationUuid, TENANT_UUID, TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID,
            INVITEE_EMAIL, TENANT_UUID, "MEMBER", "PENDING"
        );
        when(invitationService.findById(new InvitationId(invitationUuid))).thenReturn(invitation);

        mockMvc.perform(get("/invitations/" + invitationUuid)
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void invitationLandingRejectsDifferentActor() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();
        final InvitationRepresentation invitation = new InvitationRepresentation(
            invitationUuid, TENANT_UUID, TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID,
            INVITEE_EMAIL, TENANT_UUID, "MEMBER", "PENDING"
        );
        when(invitationService.findById(new InvitationId(invitationUuid))).thenReturn(invitation);

        mockMvc.perform(get("/invitations/" + invitationUuid)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void inviteCreatesInvitationAndReturnsFragment() throws Exception {
        final InvitationRepresentation inv = new InvitationRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID,
            null, null, "MEMBER", "PENDING"
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
    void inviteExternalWithDuplicateEmailReturnsFragmentWithInlineError() throws Exception {
        doThrow(new DuplicateEntityException("invitation.error.alreadyExists"))
            .when(invitationService).inviteExternal(any());
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/external")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .header("HX-Request", "true")
                .param("email", "dup@test.de")
                .param("firstName", "Dup")
                .param("lastName", "User")
                .param("dateOfBirth", "1990-01-01"))
            .andExpect(status().isConflict())
            .andExpect(view().name("trip/invitations :: invitationList"))
            .andExpect(model().attribute("invitationError",
                "Diese E-Mail-Adresse wurde für diese Reise bereits eingeladen."));
    }

    @Test
    void acceptInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();

        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/" + invitationUuid + "/accept")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL)))
                .header("HX-Request", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).accept(new InvitationId(invitationUuid), INVITEE_UUID);
    }

    @Test
    void declineInvitationReturnsFragment() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();

        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/" + invitationUuid + "/decline")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL)))
                .header("HX-Request", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("trip/invitations :: invitationList"));

        verify(invitationService).decline(new InvitationId(invitationUuid), INVITEE_UUID);
    }

    @Test
    void acceptInvitationRedirectsForRegularFormPost() throws Exception {
        final UUID invitationUuid = UUID.randomUUID();

        mockMvc.perform(post("/" + TRIP_UUID + "/invitations/" + invitationUuid + "/accept")
                .with(jwt().jwt(j -> j.claim("email", INVITEE_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(invitationService).accept(new InvitationId(invitationUuid), INVITEE_UUID);
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
    void createTripSubmitsWithoutDates() throws Exception {
        mockMvc.perform(post("/")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .param("name", "Neue Planung")
                .param("description", "Beschreibung"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(tripService).createTrip(new de.evia.travelmate.trips.application.command.CreateTripCommand(
            TENANT_UUID, "Neue Planung", "Beschreibung", ORGANIZER_UUID
        ));
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
            TRIP_UUID, ORGANIZER_UUID, ORGANIZER_UUID, TENANT_UUID,
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)));
    }

    @Test
    void addOwnParticipantRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/participants")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .param("participantId", DEPENDENT_UUID.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(tripService).addParticipantToTrip(new AddParticipantToTripCommand(
            TRIP_UUID, DEPENDENT_UUID, ORGANIZER_UUID, TENANT_UUID
        ));
    }

    @Test
    void removeOwnParticipantRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/participants/" + DEPENDENT_UUID + "/remove")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(tripService).removeParticipantFromTrip(new RemoveParticipantFromTripCommand(
            TRIP_UUID, DEPENDENT_UUID, ORGANIZER_UUID, TENANT_UUID
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void detailExcludesExistingParticipantsFromAvailableOwnPartyDropdown() throws Exception {
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID,
            List.of(ORGANIZER_UUID, INVITEE_UUID, DEPENDENT_UUID)
        );

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("availableOwnPartyParticipants"))
            .andDo(result -> {
                final List<PartyParticipantOption> available =
                    (List<PartyParticipantOption>) result.getModelAndView()
                        .getModel().get("availableOwnPartyParticipants");
                assertThat(available).isEmpty();
            });
    }

    @SuppressWarnings("unchecked")
    @Test
    void detailShowsOnlyNonParticipantsInAvailableOwnPartyDropdown() throws Exception {
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID,
            List.of(ORGANIZER_UUID, INVITEE_UUID)
        );

        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);
        when(invitationService.findByTripId(new TripId(TRIP_UUID))).thenReturn(List.of());

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andDo(result -> {
                final List<PartyParticipantOption> available =
                    (List<PartyParticipantOption>) result.getModelAndView()
                        .getModel().get("availableOwnPartyParticipants");
                assertThat(available).hasSize(1);
                assertThat(available.getFirst().participantId()).isEqualTo(DEPENDENT_UUID);
            });
    }

    @Test
    void grantOrganizerRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/organizers/" + INVITEE_UUID)
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(tripService).grantTripOrganizer(new GrantTripOrganizerCommand(
            TRIP_UUID, INVITEE_UUID, ORGANIZER_UUID
        ));
    }

    @Test
    void editFormRendersEditView() throws Exception {
        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            "PLANNING", ORGANIZER_UUID, List.of(ORGANIZER_UUID)
        );
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);

        mockMvc.perform(get("/" + TRIP_UUID + "/edit")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "trip/edit"))
            .andExpect(model().attributeExists("trip"));
    }

    @Test
    void editSubmitInvokesServiceAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/edit")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL)))
                .param("name", "Skiurlaub 2026")
                .param("description", "Aktualisierte Beschreibung"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(tripService).editTrip(new EditTripCommand(
            TRIP_UUID, "Skiurlaub 2026", "Aktualisierte Beschreibung"
        ));
    }

    @Test
    void deleteInvokesServiceAndRedirectsToList() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/delete")
                .with(jwt().jwt(j -> j.claim("email", ORGANIZER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(tripService).deleteTrip(new TripId(TRIP_UUID));
    }
}
