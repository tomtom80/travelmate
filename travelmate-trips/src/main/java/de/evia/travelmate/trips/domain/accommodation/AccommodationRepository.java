package de.evia.travelmate.trips.domain.accommodation;

import java.util.Optional;

import de.evia.travelmate.trips.domain.trip.TripId;

public interface AccommodationRepository {

    Accommodation save(Accommodation accommodation);

    Optional<Accommodation> findByTripId(TripId tripId);

    boolean existsByTripId(TripId tripId);

    void deleteByTripId(TripId tripId);
}
