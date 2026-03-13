package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripJpaRepository extends JpaRepository<TripJpaEntity, UUID> {

    List<TripJpaEntity> findAllByTenantId(UUID tenantId);

    @Query("SELECT DISTINCT t FROM TripJpaEntity t JOIN t.participants p WHERE p.participantId = :participantId")
    List<TripJpaEntity> findAllByParticipantId(@Param("participantId") UUID participantId);
}
