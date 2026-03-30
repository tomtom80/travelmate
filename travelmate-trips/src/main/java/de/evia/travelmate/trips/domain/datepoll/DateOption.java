package de.evia.travelmate.trips.domain.datepoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

import de.evia.travelmate.trips.domain.trip.DateRange;

public class DateOption {

    private final DateOptionId dateOptionId;
    private final DateRange dateRange;

    public DateOption(final DateRange dateRange) {
        argumentIsNotNull(dateRange, "dateRange");
        this.dateOptionId = new DateOptionId(UUID.randomUUID());
        this.dateRange = dateRange;
    }

    public DateOption(final DateOptionId dateOptionId, final DateRange dateRange) {
        argumentIsNotNull(dateOptionId, "dateOptionId");
        argumentIsNotNull(dateRange, "dateRange");
        this.dateOptionId = dateOptionId;
        this.dateRange = dateRange;
    }

    public DateOptionId dateOptionId() {
        return dateOptionId;
    }

    public DateRange dateRange() {
        return dateRange;
    }
}
