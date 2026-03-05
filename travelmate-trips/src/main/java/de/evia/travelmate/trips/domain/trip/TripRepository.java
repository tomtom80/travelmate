package de.evia.travelmate.trips.domain.trip;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;

public interface TripRepository {

    Trip save(Trip trip);

    Optional<Trip> findById(TripId tripId);

    List<Trip> findAllByTenantId(TenantId tenantId);
}
