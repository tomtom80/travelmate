package de.evia.travelmate.trips.application.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateDatePollCommand(
    UUID tenantId,
    UUID tripId,
    List<DateRangeCommand> dateRanges
) {

    public record DateRangeCommand(LocalDate startDate, LocalDate endDate) {
    }
}
