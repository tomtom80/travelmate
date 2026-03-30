package de.evia.travelmate.trips.domain.datepoll;

import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public interface DatePollRepository {

    DatePoll save(DatePoll datePoll);

    Optional<DatePoll> findById(TenantId tenantId, DatePollId datePollId);

    Optional<DatePoll> findOpenByTripId(TenantId tenantId, TripId tripId);

    Optional<DatePoll> findLatestByTripId(TenantId tenantId, TripId tripId);

    void delete(DatePoll datePoll);
}
