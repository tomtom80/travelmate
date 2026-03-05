package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record TripId(UUID value) {

    public TripId {
        argumentIsNotNull(value, "tripId");
    }
}
