package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.trips.application.command.AddRoomCommand;
import de.evia.travelmate.trips.application.command.AssignPartyToRoomCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomAssignmentCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomCommand;
import de.evia.travelmate.trips.application.command.RoomCommand;
import de.evia.travelmate.trips.application.command.SetAccommodationCommand;
import de.evia.travelmate.trips.application.representation.AccommodationRepresentation;
import de.evia.travelmate.trips.domain.accommodation.Accommodation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportPort;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.AccommodationRepository;
import de.evia.travelmate.trips.domain.accommodation.ImportedRoom;
import de.evia.travelmate.trips.domain.accommodation.Room;
import de.evia.travelmate.trips.domain.accommodation.RoomType;
import de.evia.travelmate.trips.domain.trip.TripId;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private AccommodationImportPort accommodationImportPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccommodationService accommodationService;

    @Test
    void setAccommodationCreatesNew() {
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.empty());
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));

        final SetAccommodationCommand command = new SetAccommodationCommand(
            TENANT_UUID, TRIP_UUID, "Berghuette", "Alpweg 12", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            new BigDecimal("3000.00"),
            List.of(new RoomCommand("Zimmer 1", "DOUBLE", 2, new BigDecimal("80.00")))
        );

        final AccommodationRepresentation result = accommodationService.setAccommodation(command);

        assertThat(result.name()).isEqualTo("Berghuette");
        assertThat(result.rooms()).hasSize(1);
        assertThat(result.assignments()).isEmpty();
        verify(accommodationRepository).save(any(Accommodation.class));
        verify(eventPublisher).publishEvent(any(AccommodationPriceSet.class));
    }

    @Test
    void setAccommodationUpdatesExisting() {
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Old Name", null, null,
            null, null, null,
            List.of(new Room("Old Room", RoomType.SINGLE, 1, null))
        );
        existing.clearDomainEvents();
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));

        final SetAccommodationCommand command = new SetAccommodationCommand(
            TENANT_UUID, TRIP_UUID, "New Name", null, null,
            null, null, new BigDecimal("500.00"),
            List.of(new RoomCommand("New Room", "DOUBLE", 2, null))
        );

        final AccommodationRepresentation result = accommodationService.setAccommodation(command);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(accommodationRepository).save(any(Accommodation.class));
    }

    @Test
    void addRoomToExistingAccommodation() {
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", RoomType.DOUBLE, 2, null))
        );
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));

        final AddRoomCommand command = new AddRoomCommand(
            TENANT_UUID, TRIP_UUID, "Zimmer 2", "QUAD", 4, null
        );

        final AccommodationRepresentation result = accommodationService.addRoom(command);

        assertThat(result.rooms()).hasSize(2);
        verify(accommodationRepository).save(any(Accommodation.class));
    }

    @Test
    void addRoomThrowsWhenNoAccommodation() {
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accommodationService.addRoom(
            new AddRoomCommand(TENANT_UUID, TRIP_UUID, "Zimmer", "SINGLE", 1, null)
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void removeRoomFromAccommodation() {
        final Room room1 = new Room("Zimmer 1", RoomType.DOUBLE, 2, null);
        final Room room2 = new Room("Zimmer 2", RoomType.QUAD, 4, null);
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Huette", null, null,
            null, null, null, List.of(room1, room2)
        );
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));

        final AccommodationRepresentation result = accommodationService.removeRoom(
            new RemoveRoomCommand(TENANT_UUID, TRIP_UUID, room1.roomId().value())
        );

        assertThat(result.rooms()).hasSize(1);
    }

    @Test
    void assignPartyToRoom() {
        final Room room = new Room("Zimmer 1", RoomType.DOUBLE, 2, null);
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Huette", null, null,
            null, null, null, List.of(room)
        );
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));
        final UUID partyId = UUID.randomUUID();

        final AccommodationRepresentation result = accommodationService.assignPartyToRoom(
            new AssignPartyToRoomCommand(TENANT_UUID, TRIP_UUID, room.roomId().value(), partyId, "Familie Schmidt", 2)
        );

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().partyName()).isEqualTo("Familie Schmidt");
        assertThat(result.assignments().getFirst().personCount()).isEqualTo(2);
        assertThat(result.totalAssignedPersons()).isEqualTo(2);
        verify(accommodationRepository).save(any(Accommodation.class));
    }

    @Test
    void assignPartyToRoomThrowsWhenNoAccommodation() {
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accommodationService.assignPartyToRoom(
            new AssignPartyToRoomCommand(TENANT_UUID, TRIP_UUID, UUID.randomUUID(), UUID.randomUUID(), "Test", 1)
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void removeRoomAssignment() {
        final Room room = new Room("Zimmer 1", RoomType.DOUBLE, 2, null);
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Huette", null, null,
            null, null, null, List.of(room)
        );
        existing.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Familie Schmidt", 2);
        final UUID assignmentId = existing.assignments().getFirst().assignmentId().value();
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(inv -> inv.getArgument(0));

        final AccommodationRepresentation result = accommodationService.removeRoomAssignment(
            new RemoveRoomAssignmentCommand(TENANT_UUID, TRIP_UUID, assignmentId)
        );

        assertThat(result.assignments()).isEmpty();
        assertThat(result.totalAssignedPersons()).isEqualTo(0);
        verify(accommodationRepository).save(any(Accommodation.class));
    }

    @Test
    void removeAccommodationDeletesByTripId() {
        final TripId tripId = new TripId(TRIP_UUID);

        accommodationService.removeAccommodation(tripId);

        verify(accommodationRepository).deleteByTripId(tripId);
    }

    @Test
    void findByTripIdReturnsRepresentation() {
        final Accommodation existing = Accommodation.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID), "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", RoomType.DOUBLE, 2, null))
        );
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.of(existing));

        final Optional<AccommodationRepresentation> result = accommodationService.findByTripId(new TripId(TRIP_UUID));

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Huette");
    }

    @Test
    void findByTripIdReturnsEmptyWhenNotFound() {
        when(accommodationRepository.findByTripId(new TripId(TRIP_UUID))).thenReturn(Optional.empty());

        final Optional<AccommodationRepresentation> result = accommodationService.findByTripId(new TripId(TRIP_UUID));

        assertThat(result).isEmpty();
    }

    @Test
    void existsByTripIdDelegatesToRepository() {
        when(accommodationRepository.existsByTripId(new TripId(TRIP_UUID))).thenReturn(true);

        assertThat(accommodationService.existsByTripId(new TripId(TRIP_UUID))).isTrue();
    }

    @Test
    void importFromUrlDelegatesToPort() {
        final String url = "https://www.huetten.com/chalet";
        final AccommodationImportResult importResult = new AccommodationImportResult(
            "Chalet am Kogl", "Kogl 32, 8551 Wies", url,
            null, null, new BigDecimal("4025"), null,
            List.of(new ImportedRoom("Schlafzimmer 1", RoomType.DOUBLE, 2, null))
        );
        when(accommodationImportPort.importFromUrl(url)).thenReturn(Optional.of(importResult));

        final Optional<AccommodationImportResult> result = accommodationService.importFromUrl(url);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Chalet am Kogl");
        verify(accommodationImportPort).importFromUrl(url);
    }

    @Test
    void importFromUrlReturnsEmptyWhenPortReturnsEmpty() {
        final String url = "https://example.com/nothing";
        when(accommodationImportPort.importFromUrl(url)).thenReturn(Optional.empty());

        final Optional<AccommodationImportResult> result = accommodationService.importFromUrl(url);

        assertThat(result).isEmpty();
    }
}
