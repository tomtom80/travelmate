package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record TripName(String value) {

    private static final int MAX_LENGTH = 255;

    public TripName {
        argumentIsNotBlank(value, "tripName");
        argumentIsTrue(value.length() <= MAX_LENGTH,
            "Trip name must not exceed " + MAX_LENGTH + " characters.");
    }
}
