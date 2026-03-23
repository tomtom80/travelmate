package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Participant;
import de.evia.travelmate.trips.domain.trip.StayPeriod;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;
import de.evia.travelmate.trips.domain.trip.TripStatus;

@Repository
public class TripRepositoryAdapter implements TripRepository {

    private final TripJpaRepository jpaRepository;

    public TripRepositoryAdapter(final TripJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Trip save(final Trip trip) {
        final TripJpaEntity entity = jpaRepository.findById(trip.tripId().value())
            .orElseGet(() -> new TripJpaEntity(
                trip.tripId().value(), trip.tenantId().value(),
                trip.name().value(), trip.description(),
                trip.dateRange().startDate(), trip.dateRange().endDate(),
                trip.status().name(), trip.organizerId(), trip.organizerIds()
            ));
        entity.setStatus(trip.status().name());
        syncOrganizerIds(entity, trip);
        syncParticipants(entity, trip);
        jpaRepository.save(entity);
        return trip;
    }

    @Override
    public Optional<Trip> findById(final TripId tripId) {
        return jpaRepository.findById(tripId.value()).map(this::toDomain);
    }

    @Override
    public List<Trip> findAllByTenantId(final TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Trip> findAllByParticipantId(final UUID participantId) {
        return jpaRepository.findAllByParticipantId(participantId).stream()
            .map(this::toDomain)
            .toList();
    }

    private void syncParticipants(final TripJpaEntity entity, final Trip trip) {
        for (final Participant participant : trip.participants()) {
            final Optional<ParticipantJpaEntity> existing = entity.getParticipants().stream()
                .filter(p -> p.getParticipantId().equals(participant.participantId()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setFirstName(participant.firstName());
                existing.get().setLastName(participant.lastName());
                existing.get().setArrivalDate(
                    participant.stayPeriod() != null ? participant.stayPeriod().arrivalDate() : null);
                existing.get().setDepartureDate(
                    participant.stayPeriod() != null ? participant.stayPeriod().departureDate() : null);
            } else {
                entity.getParticipants().add(new ParticipantJpaEntity(
                    participant.participantId(), entity,
                    participant.firstName(), participant.lastName(),
                    participant.stayPeriod() != null ? participant.stayPeriod().arrivalDate() : null,
                    participant.stayPeriod() != null ? participant.stayPeriod().departureDate() : null
                ));
            }
        }
        entity.getParticipants().removeIf(existing ->
            trip.participants().stream().noneMatch(p -> p.participantId().equals(existing.getParticipantId())));
    }

    private void syncOrganizerIds(final TripJpaEntity entity, final Trip trip) {
        entity.getOrganizerIds().clear();
        entity.getOrganizerIds().addAll(trip.organizerIds());
    }

    private Trip toDomain(final TripJpaEntity entity) {
        final var participants = entity.getParticipants().stream()
            .map(p -> {
                final StayPeriod stayPeriod = (p.getArrivalDate() != null && p.getDepartureDate() != null)
                    ? new StayPeriod(p.getArrivalDate(), p.getDepartureDate()) : null;
                return new Participant(p.getParticipantId(), p.getFirstName(), p.getLastName(), stayPeriod);
            })
            .toList();
        return new Trip(
            new TripId(entity.getTripId()),
            new TenantId(entity.getTenantId()),
            new TripName(entity.getName()),
            entity.getDescription(),
            new DateRange(entity.getStartDate(), entity.getEndDate()),
            entity.getOrganizerId(),
            entity.getOrganizerIds().isEmpty() ? List.of(entity.getOrganizerId()) : entity.getOrganizerIds(),
            TripStatus.valueOf(entity.getStatus()),
            participants
        );
    }
}
