package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record AccommodationVoteId(UUID value) {

    public AccommodationVoteId {
        argumentIsNotNull(value, "accommodationVoteId");
    }
}
