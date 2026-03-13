package de.evia.travelmate.expense.domain.trip;

import java.util.Optional;
import java.util.UUID;

public interface TripProjectionRepository {

    TripProjection save(TripProjection projection);

    Optional<TripProjection> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);
}
