package de.evia.travelmate.expense.adapters.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@Repository
@Transactional
public class TripProjectionRepositoryAdapter implements TripProjectionRepository {

    private final TripProjectionJpaRepository jpaRepository;

    public TripProjectionRepositoryAdapter(final TripProjectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TripProjection save(final TripProjection projection) {
        final TripProjectionJpaEntity entity = jpaRepository.findById(projection.tripId())
            .orElseGet(() -> new TripProjectionJpaEntity(
                projection.tripId(),
                projection.tenantId().value(),
                projection.tripName()
            ));
        entity.setTripName(projection.tripName());
        entity.setStartDate(projection.startDate());
        entity.setEndDate(projection.endDate());
        entity.setAccommodationTotalPrice(projection.accommodationTotalPrice());
        syncParticipants(entity, projection);
        jpaRepository.save(entity);
        return projection;
    }

    @Override
    public Optional<TripProjection> findByTripId(final UUID tripId) {
        return jpaRepository.findById(tripId).map(this::toDomain);
    }

    @Override
    public List<TripProjection> findByPartyTenantId(final UUID partyTenantId) {
        return jpaRepository.findDistinctByParticipantsPartyTenantId(partyTenantId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsByTripId(final UUID tripId) {
        return jpaRepository.existsByTripId(tripId);
    }

    @Override
    public void deleteByTripId(final UUID tripId) {
        jpaRepository.deleteByTripId(tripId);
    }

    private void syncParticipants(final TripProjectionJpaEntity entity,
                                  final TripProjection projection) {
        entity.getParticipants().removeIf(p ->
            projection.participants().stream()
                .noneMatch(dp -> dp.participantId().equals(p.getParticipantId())));

        for (final TripParticipant participant : projection.participants()) {
            final Optional<TripParticipantJpaEntity> existing = entity.getParticipants().stream()
                .filter(p -> p.getParticipantId().equals(participant.participantId()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setName(participant.name());
                existing.get().setArrivalDate(participant.arrivalDate());
                existing.get().setDepartureDate(participant.departureDate());
                existing.get().setPartyTenantId(participant.partyTenantId());
                existing.get().setPartyName(participant.partyName());
                existing.get().setDateOfBirth(participant.dateOfBirth());
                existing.get().setAccountHolder(participant.accountHolder());
            } else {
                final TripParticipantJpaEntity newEntity = new TripParticipantJpaEntity(
                    entity, participant.participantId(), participant.name()
                );
                newEntity.setArrivalDate(participant.arrivalDate());
                newEntity.setDepartureDate(participant.departureDate());
                newEntity.setPartyTenantId(participant.partyTenantId());
                newEntity.setPartyName(participant.partyName());
                newEntity.setDateOfBirth(participant.dateOfBirth());
                newEntity.setAccountHolder(participant.accountHolder());
                entity.getParticipants().add(newEntity);
            }
        }
    }

    private TripProjection toDomain(final TripProjectionJpaEntity entity) {
        final var participants = entity.getParticipants().stream()
            .map(p -> new TripParticipant(p.getParticipantId(), p.getName(),
                p.getArrivalDate(), p.getDepartureDate(),
                p.getPartyTenantId(), p.getPartyName(), p.getDateOfBirth(), p.isAccountHolder()))
            .toList();
        final TripProjection projection = new TripProjection(
            entity.getTripId(),
            new TenantId(entity.getTenantId()),
            entity.getTripName(),
            entity.getStartDate(),
            entity.getEndDate(),
            participants
        );
        projection.setAccommodationTotalPrice(entity.getAccommodationTotalPrice());
        return projection;
    }
}
