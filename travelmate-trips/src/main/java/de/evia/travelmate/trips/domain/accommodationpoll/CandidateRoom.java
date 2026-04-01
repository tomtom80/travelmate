package de.evia.travelmate.trips.domain.accommodationpoll;

import java.math.BigDecimal;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record CandidateRoom(String name, int bedCount, BigDecimal pricePerNight, String bedDescription) {

    public CandidateRoom {
        argumentIsNotNull(name, "name");
        argumentIsNotBlank(name, "name");
        argumentIsTrue(bedCount > 0, "bedCount must be positive.");
    }
}
