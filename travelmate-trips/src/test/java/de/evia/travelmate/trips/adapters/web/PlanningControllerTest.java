package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanningControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "organizer@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private DatePollService datePollService;

    @MockitoBean
    private AccommodationPollService accommodationPollService;

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
    void showsPlanningOverviewWithBothPolls() throws Exception {
        when(datePollService.findLatestByTripId(any(), any())).thenReturn(createDatePollRepr());
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createAccommodationPollRepr());

        mockMvc.perform(get("/" + TRIP_UUID + "/planning")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "planning/overview"))
            .andExpect(model().attributeExists("datePoll"))
            .andExpect(model().attributeExists("accommodationPoll"))
            .andExpect(model().attributeExists("trip"));
    }

    @Test
    void showsPlanningOverviewWithNoPolls() throws Exception {
        when(datePollService.findLatestByTripId(any(), any()))
            .thenThrow(new EntityNotFoundException("DatePoll", "test"));
        when(accommodationPollService.findLatestByTripId(any(), any()))
            .thenThrow(new EntityNotFoundException("AccommodationPoll", "test"));

        mockMvc.perform(get("/" + TRIP_UUID + "/planning")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attribute("datePoll", (Object) null))
            .andExpect(model().attribute("accommodationPoll", (Object) null));
    }

    @Test
    void showsPlanningOverviewWithOnlyDatePoll() throws Exception {
        when(datePollService.findLatestByTripId(any(), any())).thenReturn(createDatePollRepr());
        when(accommodationPollService.findLatestByTripId(any(), any()))
            .thenThrow(new EntityNotFoundException("AccommodationPoll", "test"));

        mockMvc.perform(get("/" + TRIP_UUID + "/planning")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("datePoll"))
            .andExpect(model().attribute("accommodationPoll", (Object) null));
    }

    @Test
    void rejectsCrossTenantAccess() throws Exception {
        final UUID foreignTrip = UUID.randomUUID();
        final TripRepresentation trip = new TripRepresentation(
            foreignTrip, UUID.randomUUID(), "Foreign", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
            "PLANNING", UUID.randomUUID(), List.of());
        when(tripService.findById(new TripId(foreignTrip))).thenReturn(trip);

        mockMvc.perform(get("/" + foreignTrip + "/planning")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitedParticipantFromAnotherTravelPartySeesPlanningWithTripTenantPolls() throws Exception {
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
        when(datePollService.findLatestByTripId(any(), any())).thenReturn(createDatePollRepr());
        when(accommodationPollService.findLatestByTripId(any(), any())).thenReturn(createAccommodationPollRepr());

        mockMvc.perform(get("/" + TRIP_UUID + "/planning")
                .with(jwt().jwt(j -> j.claim("email", participantEmail))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("datePoll"))
            .andExpect(model().attributeExists("accommodationPoll"));
    }

    private DatePollRepresentation createDatePollRepr() {
        return new DatePollRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, "OPEN", null,
            List.of(
                new DatePollRepresentation.DateOptionRepresentation(
                    UUID.randomUUID(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14), 2),
                new DatePollRepresentation.DateOptionRepresentation(
                    UUID.randomUUID(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14), 1)
            ),
            List.of()
        );
    }

    private AccommodationPollRepresentation createAccommodationPollRepr() {
        return new AccommodationPollRepresentation(
            UUID.randomUUID(), TENANT_UUID, TRIP_UUID, "OPEN", null, null, null,
            List.of(
                new AccommodationPollRepresentation.CandidateRepresentation(
                    UUID.randomUUID(), "Hotel A", "https://a.com", "Nice", 1,
                    List.of(new AccommodationPollRepresentation.RoomRepresentation("Room A", 2, null, null)),
                    Set.of()),
                new AccommodationPollRepresentation.CandidateRepresentation(
                    UUID.randomUUID(), "Hotel B", null, "Cozy", 0,
                    List.of(new AccommodationPollRepresentation.RoomRepresentation("Room B", 3, null, null)),
                    Set.of())
            ),
            List.of()
        );
    }
}
