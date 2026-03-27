package de.evia.travelmate.expense.domain.trip;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TripProjectionRepository {

    TripProjection save(TripProjection projection);

    Optional<TripProjection> findByTripId(UUID tripId);

    List<TripProjection> findByPartyTenantId(UUID partyTenantId);

    boolean existsByTripId(UUID tripId);
}
