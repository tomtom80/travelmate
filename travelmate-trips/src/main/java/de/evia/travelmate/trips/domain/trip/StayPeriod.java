package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.time.LocalDate;

public record StayPeriod(LocalDate arrivalDate, LocalDate departureDate) {

    public StayPeriod {
        argumentIsNotNull(arrivalDate, "arrivalDate");
        argumentIsNotNull(departureDate, "departureDate");
        argumentIsTrue(!departureDate.isBefore(arrivalDate),
            "Departure date must not be before arrival date.");
    }

    public boolean isWithin(final DateRange tripRange) {
        return tripRange.contains(arrivalDate) && tripRange.contains(departureDate);
    }
}
