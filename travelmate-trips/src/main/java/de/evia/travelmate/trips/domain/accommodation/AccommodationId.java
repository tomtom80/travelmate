package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record AccommodationId(UUID value) {

    public AccommodationId {
        argumentIsNotNull(value, "accommodationId");
    }
}
