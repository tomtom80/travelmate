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
import java.util.Set;
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
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CastDateVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatePollControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID DATE_POLL_UUID = UUID.randomUUID();
    private static final UUID OPTION_1_UUID = UUID.randomUUID();
    private static final UUID OPTION_2_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "organizer@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatePollService datePollService;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @BeforeEach
    void setUpPartyAndTrip() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(MEMBER_UUID, MEMBER_EMAIL, "Max", "Organizer");
        when(travelPartyRepository.findByMemberEmail(MEMBER_EMAIL)).thenReturn(Optional.of(party));

        final TripRepresentation tripRepr = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Test Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(tripRepr);
    }

    @Test
    void overviewShowsDatePollPage() throws Exception {
        when(datePollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(get("/" + TRIP_UUID + "/datepoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "datepoll/overview"))
            .andExpect(model().attributeExists("datePoll"))
            .andExpect(model().attributeExists("trip"));
    }

    @Test
    void overviewShowsEmptyStateWhenNoPoll() throws Exception {
        when(datePollService.findLatestByTripId(any(), any()))
            .thenThrow(new de.evia.travelmate.common.domain.EntityNotFoundException("DatePoll", "test"));

        mockMvc.perform(get("/" + TRIP_UUID + "/datepoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attribute("datePoll", (Object) null));
    }

    @Test
    void createFormShowsCreatePage() throws Exception {
        mockMvc.perform(get("/" + TRIP_UUID + "/datepoll/create")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "datepoll/create"));
    }

    @Test
    void createRedirectsToOverview() throws Exception {
        when(datePollService.createDatePoll(any(CreateDatePollCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/datepoll/create")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("startDate", "2026-07-01", "2026-08-01")
                .param("endDate", "2026-07-14", "2026-08-14"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/datepoll"));

        verify(datePollService).createDatePoll(any(CreateDatePollCommand.class));
    }

    @Test
    void castVoteRedirectsToOverview() throws Exception {
        when(datePollService.castVote(any(CastDateVoteCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/datepoll/" + DATE_POLL_UUID + "/vote")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("selectedOptionIds", OPTION_1_UUID.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/datepoll"));

        verify(datePollService).castVote(any(CastDateVoteCommand.class));
    }

    @Test
    void confirmRedirectsToOverview() throws Exception {
        when(datePollService.confirmPoll(any(ConfirmDatePollCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/datepoll/" + DATE_POLL_UUID + "/confirm")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("confirmedOptionId", OPTION_1_UUID.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/datepoll"));

        verify(datePollService).confirmPoll(any(ConfirmDatePollCommand.class));
    }

    @Test
    void cancelRedirectsToOverview() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/datepoll/" + DATE_POLL_UUID + "/cancel")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/datepoll"));

        verify(datePollService).cancelPoll(new TenantId(TENANT_UUID), new DatePollId(DATE_POLL_UUID));
    }

    @Test
    void overviewRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final UUID otherTenantUuid = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, otherTenantUuid, "Foreign Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(get("/" + otherTenantTrip + "/datepoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createRejectsNonOrganizer() throws Exception {
        final UUID participantUuid = UUID.randomUUID();
        final String participantEmail = "participant@test.de";
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(participantUuid, participantEmail, "Jane", "Participant");
        when(travelPartyRepository.findByMemberEmail(participantEmail)).thenReturn(Optional.of(party));

        final TripRepresentation trip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Test Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID, participantUuid));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(trip);

        mockMvc.perform(get("/" + TRIP_UUID + "/datepoll/create")
                .with(jwt().jwt(j -> j.claim("email", participantEmail))))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitedParticipantFromAnotherTravelPartyCanSeeSharedDatePoll() throws Exception {
        final UUID participantTenantUuid = UUID.randomUUID();
        final UUID participantUuid = UUID.randomUUID();
        final String participantEmail = "participant-other-party@test.de";

        final TravelParty participantParty = TravelParty.create(new TenantId(participantTenantUuid), "Other Party");
        participantParty.addMember(participantUuid, participantEmail, "Jane", "Participant");
        when(travelPartyRepository.findByMemberEmail(participantEmail)).thenReturn(Optional.of(participantParty));

        final TripRepresentation sharedTrip = new TripRepresentation(
            TRIP_UUID, TENANT_UUID, "Shared Trip", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
            "PLANNING", MEMBER_UUID, List.of(MEMBER_UUID, participantUuid));
        when(tripService.findById(new TripId(TRIP_UUID))).thenReturn(sharedTrip);
        when(datePollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(get("/" + TRIP_UUID + "/datepoll")
                .with(jwt().jwt(j -> j.claim("email", participantEmail))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("datePoll"));

        verify(datePollService).findLatestByTripId(new TenantId(TENANT_UUID), new TripId(TRIP_UUID));
    }

    private DatePollRepresentation createPollRepresentation() {
        return new DatePollRepresentation(
            DATE_POLL_UUID, TENANT_UUID, TRIP_UUID, "OPEN", null,
            List.of(
                new DatePollRepresentation.DateOptionRepresentation(
                    OPTION_1_UUID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14), 0),
                new DatePollRepresentation.DateOptionRepresentation(
                    OPTION_2_UUID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14), 0)
            ),
            List.of()
        );
    }
}
