package de.evia.travelmate.trips.domain.datepoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record DateOptionId(UUID value) {

    public DateOptionId {
        argumentIsNotNull(value, "dateOptionId");
    }
}
