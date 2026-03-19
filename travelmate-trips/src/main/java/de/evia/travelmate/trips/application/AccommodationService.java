package de.evia.travelmate.trips.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
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
import de.evia.travelmate.trips.domain.accommodation.Room;
import de.evia.travelmate.trips.domain.accommodation.RoomAssignmentId;
import de.evia.travelmate.trips.domain.accommodation.RoomId;
import de.evia.travelmate.trips.domain.trip.TripId;

@Service
@Transactional
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationImportPort accommodationImportPort;
    private final ApplicationEventPublisher eventPublisher;

    public AccommodationService(final AccommodationRepository accommodationRepository,
                                final AccommodationImportPort accommodationImportPort,
                                final ApplicationEventPublisher eventPublisher) {
        this.accommodationRepository = accommodationRepository;
        this.accommodationImportPort = accommodationImportPort;
        this.eventPublisher = eventPublisher;
    }

    public Optional<AccommodationImportResult> importFromUrl(final String url) {
        return accommodationImportPort.importFromUrl(url);
    }

    public AccommodationRepresentation setAccommodation(final SetAccommodationCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final TenantId tenantId = new TenantId(command.tenantId());

        final List<Room> rooms = command.rooms().stream()
            .map(this::toRoom)
            .toList();

        final Optional<Accommodation> existing = accommodationRepository.findByTripId(tripId);
        final Accommodation accommodation;
        if (existing.isPresent()) {
            accommodation = existing.get();
            accommodation.updateDetails(
                command.name(), command.address(), command.url(),
                command.checkIn(), command.checkOut(), command.totalPrice()
            );
            syncRooms(accommodation, rooms);
        } else {
            accommodation = Accommodation.create(
                tenantId, tripId, command.name(), command.address(), command.url(),
                command.checkIn(), command.checkOut(), command.totalPrice(), rooms
            );
        }

        accommodationRepository.save(accommodation);
        accommodation.domainEvents().forEach(eventPublisher::publishEvent);
        accommodation.clearDomainEvents();
        return new AccommodationRepresentation(accommodation);
    }

    public AccommodationRepresentation addRoom(final AddRoomCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final Accommodation accommodation = findAccommodation(tripId);
        final Room room = new Room(
            command.name(),
            command.bedCount(),
            command.pricePerNight()
        );
        accommodation.addRoom(room);
        accommodationRepository.save(accommodation);
        return new AccommodationRepresentation(accommodation);
    }

    public AccommodationRepresentation updateRoom(final TenantId tenantId,
                                                     final UUID tripId,
                                                     final UUID roomId,
                                                     final String name,
                                                     final int bedCount) {
        final Accommodation accommodation = findAccommodation(new TripId(tripId));
        accommodation.updateRoom(new RoomId(roomId), name, bedCount);
        accommodationRepository.save(accommodation);
        return new AccommodationRepresentation(accommodation);
    }

    public AccommodationRepresentation removeRoom(final RemoveRoomCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final Accommodation accommodation = findAccommodation(tripId);
        accommodation.removeRoom(new RoomId(command.roomId()));
        accommodationRepository.save(accommodation);
        return new AccommodationRepresentation(accommodation);
    }

    public AccommodationRepresentation assignPartyToRoom(final AssignPartyToRoomCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final Accommodation accommodation = findAccommodation(tripId);
        accommodation.assignPartyToRoom(
            new RoomId(command.roomId()),
            command.partyTenantId(),
            command.partyName(),
            command.personCount()
        );
        accommodationRepository.save(accommodation);
        return new AccommodationRepresentation(accommodation);
    }

    public AccommodationRepresentation removeRoomAssignment(final RemoveRoomAssignmentCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final Accommodation accommodation = findAccommodation(tripId);
        accommodation.removeAssignment(new RoomAssignmentId(command.assignmentId()));
        accommodationRepository.save(accommodation);
        return new AccommodationRepresentation(accommodation);
    }

    public void removeAccommodation(final TripId tripId) {
        accommodationRepository.deleteByTripId(tripId);
    }

    @Transactional(readOnly = true)
    public Optional<AccommodationRepresentation> findByTripId(final TripId tripId) {
        return accommodationRepository.findByTripId(tripId)
            .map(AccommodationRepresentation::new);
    }

    @Transactional(readOnly = true)
    public boolean existsByTripId(final TripId tripId) {
        return accommodationRepository.existsByTripId(tripId);
    }

    private Accommodation findAccommodation(final TripId tripId) {
        return accommodationRepository.findByTripId(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Accommodation", tripId.value().toString()));
    }

    private void syncRooms(final Accommodation accommodation, final List<Room> newRooms) {
        // Clear existing rooms and add new ones
        // Since we rebuild rooms from command, we replace all rooms
        final List<RoomId> existingIds = accommodation.rooms().stream()
            .map(Room::roomId)
            .toList();

        // Add new rooms first, then remove old ones
        for (final Room newRoom : newRooms) {
            accommodation.addRoom(newRoom);
        }
        for (final RoomId oldId : existingIds) {
            accommodation.removeRoom(oldId);
        }
    }

    private Room toRoom(final RoomCommand command) {
        return new Room(
            command.name(),
            command.bedCount(),
            command.pricePerNight()
        );
    }
}
