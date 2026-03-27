package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripParticipationJpaRepository extends JpaRepository<TripParticipationJpaEntity, TripParticipationJpaEntity.TripParticipationId> {

    boolean existsByParticipantId(UUID participantId);
}
