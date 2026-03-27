package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.iam.domain.tripparticipation.TripParticipationRepository;

@Repository
public class TripParticipationRepositoryAdapter implements TripParticipationRepository {

    private final TripParticipationJpaRepository jpaRepository;

    public TripParticipationRepositoryAdapter(final TripParticipationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void add(final UUID participantId, final UUID tripId) {
        final TripParticipationJpaEntity.TripParticipationId id =
            new TripParticipationJpaEntity.TripParticipationId(participantId, tripId);
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(new TripParticipationJpaEntity(participantId, tripId));
        }
    }

    @Override
    public void remove(final UUID participantId, final UUID tripId) {
        jpaRepository.deleteById(new TripParticipationJpaEntity.TripParticipationId(participantId, tripId));
    }

    @Override
    public boolean existsByParticipantId(final UUID participantId) {
        return jpaRepository.existsByParticipantId(participantId);
    }
}
