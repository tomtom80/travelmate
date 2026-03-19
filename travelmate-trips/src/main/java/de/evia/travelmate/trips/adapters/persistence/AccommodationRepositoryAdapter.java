package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.accommodation.Accommodation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationId;
import de.evia.travelmate.trips.domain.accommodation.AccommodationRepository;
import de.evia.travelmate.trips.domain.accommodation.Room;
import de.evia.travelmate.trips.domain.accommodation.RoomAssignment;
import de.evia.travelmate.trips.domain.accommodation.RoomAssignmentId;
import de.evia.travelmate.trips.domain.accommodation.RoomId;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class AccommodationRepositoryAdapter implements AccommodationRepository {

    private final AccommodationJpaRepository jpaRepository;

    public AccommodationRepositoryAdapter(final AccommodationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Accommodation save(final Accommodation accommodation) {
        final AccommodationJpaEntity entity = jpaRepository.findById(accommodation.accommodationId().value())
            .orElseGet(() -> new AccommodationJpaEntity(
                accommodation.accommodationId().value(),
                accommodation.tenantId().value(),
                accommodation.tripId().value(),
                accommodation.name(),
                accommodation.address(),
                accommodation.url(),
                accommodation.checkIn(),
                accommodation.checkOut(),
                accommodation.totalPrice()
            ));
        entity.setName(accommodation.name());
        entity.setAddress(accommodation.address());
        entity.setUrl(accommodation.url());
        entity.setCheckIn(accommodation.checkIn());
        entity.setCheckOut(accommodation.checkOut());
        entity.setTotalPrice(accommodation.totalPrice());
        syncRooms(entity, accommodation);
        jpaRepository.save(entity);
        return accommodation;
    }

    @Override
    public Optional<Accommodation> findByTripId(final TripId tripId) {
        return jpaRepository.findByTripId(tripId.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByTripId(final TripId tripId) {
        return jpaRepository.existsByTripId(tripId.value());
    }

    @Override
    public void deleteByTripId(final TripId tripId) {
        jpaRepository.findByTripId(tripId.value()).ifPresent(jpaRepository::delete);
    }

    private void syncRooms(final AccommodationJpaEntity entity, final Accommodation accommodation) {
        entity.getRooms().clear();
        for (final Room room : accommodation.rooms()) {
            entity.getRooms().add(new AccommodationRoomJpaEntity(
                room.roomId().value(), entity,
                room.name(), room.bedCount(), room.pricePerNight()
            ));
        }
        entity.getAssignments().clear();
        for (final RoomAssignment assignment : accommodation.assignments()) {
            entity.getAssignments().add(new RoomAssignmentJpaEntity(
                assignment.assignmentId().value(), entity,
                assignment.roomId().value(),
                assignment.partyTenantId(),
                assignment.partyName(),
                assignment.personCount(),
                assignment.assignedAt()
            ));
        }
    }

    private Accommodation toDomain(final AccommodationJpaEntity entity) {
        final var rooms = entity.getRooms().stream()
            .map(r -> new Room(
                new RoomId(r.getRoomId()),
                r.getName(),
                r.getBedCount(),
                r.getPricePerNight()
            ))
            .toList();
        final List<RoomAssignment> assignments = entity.getAssignments().stream()
            .map(a -> new RoomAssignment(
                new RoomAssignmentId(a.getAssignmentId()),
                new RoomId(a.getRoomId()),
                a.getPartyTenantId(),
                a.getPartyName(),
                a.getPersonCount(),
                a.getAssignedAt()
            ))
            .toList();
        return new Accommodation(
            new AccommodationId(entity.getAccommodationId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            entity.getName(),
            entity.getAddress(),
            entity.getUrl(),
            entity.getCheckIn(),
            entity.getCheckOut(),
            entity.getTotalPrice(),
            rooms,
            assignments
        );
    }
}
