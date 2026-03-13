package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.mockito.ArgumentCaptor;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID ORGANIZER_ID = UUID.randomUUID();

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelPartyRepository travelPartyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TripService tripService;

    @Test
    void createTripReturnsTripRepresentation() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Skiurlaub", "Ab in die Berge",
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            ORGANIZER_ID
        );

        final TripRepresentation result = tripService.createTrip(command);

        assertThat(result.name()).isEqualTo("Skiurlaub");
        assertThat(result.status()).isEqualTo("PLANNING");
        assertThat(result.organizerId()).isEqualTo(ORGANIZER_ID);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTripAddsAllTravelPartyMembers() {
        final UUID member2Id = UUID.randomUUID();
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(member2Id, "lisa@example.com", "Lisa", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Sommerurlaub", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
            ORGANIZER_ID
        );

        final TripRepresentation result = tripService.createTrip(command);

        final ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        final Trip savedTrip = tripCaptor.getValue();
        assertThat(savedTrip.participants()).hasSize(3);
        assertThat(savedTrip.hasParticipant(ORGANIZER_ID)).isTrue();
        assertThat(savedTrip.hasParticipant(member2Id)).isTrue();
        assertThat(savedTrip.hasParticipant(dependentId)).isTrue();
    }

    @Test
    void createTripPublishesParticipantJoinedEventsForAllMembers() {
        final UUID member2Id = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(member2Id, "lisa@example.com", "Lisa", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Wanderung", null,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7),
            ORGANIZER_ID
        );

        tripService.createTrip(command);

        final ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        final List<ParticipantJoinedTrip> joinedEvents = eventCaptor.getAllValues().stream()
            .filter(ParticipantJoinedTrip.class::isInstance)
            .map(ParticipantJoinedTrip.class::cast)
            .toList();
        assertThat(joinedEvents).hasSize(2);
        assertThat(joinedEvents).extracting(ParticipantJoinedTrip::participantId)
            .containsExactlyInAnyOrder(ORGANIZER_ID, member2Id);
    }

    @Test
    void createTripRejectsUnknownOrganizer() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            UUID.randomUUID()
        );

        assertThatThrownBy(() -> tripService.createTrip(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void confirmTripTransitionsToConfirmed() {
        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.confirmTrip(trip.tripId());

        assertThat(trip.status().name()).isEqualTo("CONFIRMED");
        verify(tripRepository).save(trip);
    }

    @Test
    void completeTripPublishesTripCompletedEvent() {
        final Trip trip = createTrip();
        trip.clearDomainEvents();
        trip.confirm();
        trip.start();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.completeTrip(trip.tripId());

        assertThat(trip.status().name()).isEqualTo("COMPLETED");
        verify(eventPublisher).publishEvent(any(TripCompleted.class));
    }

    @Test
    void setStayPeriodUpdatesParticipant() {
        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.setStayPeriod(new SetStayPeriodCommand(
            trip.tripId().value(), ORGANIZER_ID,
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        ));

        assertThat(trip.participants().getFirst().stayPeriod()).isNotNull();
        verify(tripRepository).save(trip);
    }

    private Trip createTrip() {
        return Trip.plan(
            new TenantId(TENANT_UUID), new TripName("Skiurlaub"),
            null, new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID
        );
    }
}
