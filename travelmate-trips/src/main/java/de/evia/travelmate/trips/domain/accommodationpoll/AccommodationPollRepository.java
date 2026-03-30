package de.evia.travelmate.trips.domain.accommodationpoll;

import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public interface AccommodationPollRepository {

    AccommodationPoll save(AccommodationPoll poll);

    Optional<AccommodationPoll> findById(TenantId tenantId, AccommodationPollId pollId);

    Optional<AccommodationPoll> findOpenByTripId(TenantId tenantId, TripId tripId);

    Optional<AccommodationPoll> findLatestByTripId(TenantId tenantId, TripId tripId);

    void delete(AccommodationPoll poll);
}
