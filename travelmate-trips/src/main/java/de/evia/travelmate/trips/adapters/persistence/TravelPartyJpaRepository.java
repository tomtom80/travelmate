package de.evia.travelmate.trips.adapters.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPartyJpaRepository extends JpaRepository<TravelPartyJpaEntity, UUID> {
}
