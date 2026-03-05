package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelPartyJpaRepository extends JpaRepository<TravelPartyJpaEntity, UUID> {

    @Query("SELECT tp FROM TravelPartyJpaEntity tp JOIN tp.members m WHERE m.email = :email")
    Optional<TravelPartyJpaEntity> findByMemberEmail(@Param("email") String email);
}
