package de.evia.travelmate.trips.domain.datepoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record DateVoteId(UUID value) {

    public DateVoteId {
        argumentIsNotNull(value, "dateVoteId");
    }
}
