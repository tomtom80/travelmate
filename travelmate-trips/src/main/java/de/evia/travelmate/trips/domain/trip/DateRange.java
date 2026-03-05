package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.time.LocalDate;

public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        argumentIsNotNull(startDate, "startDate");
        argumentIsNotNull(endDate, "endDate");
        argumentIsTrue(!endDate.isBefore(startDate),
            "End date must not be before start date.");
    }

    public boolean contains(final LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
