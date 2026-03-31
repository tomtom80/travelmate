package de.evia.travelmate.trips.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.AccommodationService;
import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.CastAccommodationVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccommodationPollControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID POLL_UUID = UUID.randomUUID();
    private static final UUID CANDIDATE_1_UUID = UUID.randomUUID();
    private static final UUID CANDIDATE_2_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "organizer@test.de";
    private static final String ROOM_JSON = "[{\"name\":\"Room\",\"bedCount\":2,\"pricePerNight\":null,\"features\":\"Panorama\"}]";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccommodationService accommodationService;

    @MockitoBean
    private AccommodationPollService accommodationPollService;

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
    void overviewShowsAccommodationPollPage() throws Exception {
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(get("/" + TRIP_UUID + "/accommodationpoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "accommodationpoll/overview"))
            .andExpect(model().attributeExists("accommodationPoll"));
    }

    @Test
    void overviewShowsEmptyStateWhenNoPoll() throws Exception {
        when(accommodationPollService.findLatestByTripId(any(), any()))
            .thenThrow(new de.evia.travelmate.common.domain.EntityNotFoundException("AccommodationPoll", "test"));

        mockMvc.perform(get("/" + TRIP_UUID + "/accommodationpoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attribute("accommodationPoll", (Object) null));
    }

    @Test
    void createFormShowsCreatePage() throws Exception {
        mockMvc.perform(get("/" + TRIP_UUID + "/accommodationpoll/create")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attribute("view", "accommodationpoll/create"));
    }

    @Test
    void createRedirectsToOverview() throws Exception {
        when(accommodationPollService.createPoll(any(CreateAccommodationPollCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/create")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("candidateName", "Hotel A", "Hotel B")
                .param("candidateUrl", "https://a.com", "")
                .param("candidateDescription", "Nice", "Cozy")
                .param("candidateRoomsJson", ROOM_JSON, ROOM_JSON))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));

        final ArgumentCaptor<CreateAccommodationPollCommand> commandCaptor =
            ArgumentCaptor.forClass(CreateAccommodationPollCommand.class);
        verify(accommodationPollService).createPoll(commandCaptor.capture());
        assertThat(commandCaptor.getValue().candidates()).allSatisfy(candidate ->
            assertThat(candidate.rooms()).isNotEmpty());
    }

    @Test
    void participantCanAddCandidate() throws Exception {
        when(accommodationPollService.addCandidate(any(AddAccommodationCandidateCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/candidates/add")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Hotel C")
                .param("url", "https://c.com")
                .param("description", "Lake view")
                .param("roomsJson", ROOM_JSON))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));

        final ArgumentCaptor<AddAccommodationCandidateCommand> addCaptor =
            ArgumentCaptor.forClass(AddAccommodationCandidateCommand.class);
        verify(accommodationPollService).addCandidate(addCaptor.capture());
        assertThat(addCaptor.getValue().rooms()).isNotEmpty();
    }

    @Test
    void castVoteRedirectsToOverview() throws Exception {
        when(accommodationPollService.castVote(any(CastAccommodationVoteCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/vote")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("selectedCandidateId", CANDIDATE_1_UUID.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));

        verify(accommodationPollService).castVote(any(CastAccommodationVoteCommand.class));
    }

    @Test
    void confirmRedirectsToOverview() throws Exception {
        when(accommodationPollService.confirmPoll(any(ConfirmAccommodationPollCommand.class)))
            .thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/confirm")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("confirmedCandidateId", CANDIDATE_1_UUID.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));
    }

    @Test
    void cancelRedirectsToOverview() throws Exception {
        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/cancel")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));

        verify(accommodationPollService).cancelPoll(new TenantId(TENANT_UUID), new AccommodationPollId(POLL_UUID));
    }

    @Test
    void importCandidateRedirectsToOverview() throws Exception {
        when(accommodationService.importFromUrl("https://example.com/hotel"))
            .thenReturn(Optional.of(new AccommodationImportResult(
                "Imported Hotel", "Main Street 1", "https://example.com/hotel",
                null, null, null, "Sea view", List.of()
            )));
        when(accommodationPollService.addCandidate(any(AddAccommodationCandidateCommand.class)))
            .thenReturn(createPollRepresentation());
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "https://example.com/hotel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID + "/accommodationpoll"));

        verify(accommodationService).importFromUrl("https://example.com/hotel");
        final ArgumentCaptor<AddAccommodationCandidateCommand> importCaptor =
            ArgumentCaptor.forClass(AddAccommodationCandidateCommand.class);
        verify(accommodationPollService).addCandidate(importCaptor.capture());
        assertThat(importCaptor.getValue().rooms()).isNotEmpty();
    }

    @Test
    void importCandidateShowsValidationErrorForInvalidUrl() throws Exception {
        when(accommodationService.importFromUrl("file:///etc/passwd"))
            .thenThrow(new IllegalArgumentException("Invalid URL"));
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(post("/" + TRIP_UUID + "/accommodationpoll/" + POLL_UUID + "/import")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("url", "file:///etc/passwd"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("importError", "accommodation.import.error.url"));
    }

    @Test
    void overviewRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantTrip = UUID.randomUUID();
        final TripRepresentation foreignTrip = new TripRepresentation(
            otherTenantTrip, UUID.randomUUID(), "Foreign", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(otherTenantTrip))).thenReturn(foreignTrip);

        mockMvc.perform(get("/" + otherTenantTrip + "/accommodationpoll")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitedParticipantFromAnotherTravelPartyCanSeeSharedAccommodationPoll() throws Exception {
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
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createPollRepresentation());

        mockMvc.perform(get("/" + TRIP_UUID + "/accommodationpoll")
                .with(jwt().jwt(j -> j.claim("email", participantEmail))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("accommodationPoll"));

        verify(accommodationPollService).findLatestByTripId(new TenantId(TENANT_UUID), new TripId(TRIP_UUID));
    }

    private AccommodationPollRepresentation createPollRepresentation() {
        return new AccommodationPollRepresentation(
            POLL_UUID, TENANT_UUID, TRIP_UUID, "OPEN", null,
            List.of(
                new AccommodationPollRepresentation.CandidateRepresentation(
                    CANDIDATE_1_UUID, "Hotel A", "https://a.com", "Nice", 0,
                    List.of(new AccommodationPollRepresentation.RoomRepresentation("Room A", 2, null, "Balcony"))
                ),
                new AccommodationPollRepresentation.CandidateRepresentation(
                    CANDIDATE_2_UUID, "Hotel B", null, "Cozy", 0,
                    List.of(new AccommodationPollRepresentation.RoomRepresentation("Room B", 3, null, "Sauna"))
                )
            ),
            List.of()
        );
    }
}
