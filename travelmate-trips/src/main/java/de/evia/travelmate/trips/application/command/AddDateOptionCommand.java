package de.evia.travelmate.trips.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record AddDateOptionCommand(
    UUID tenantId,
    UUID datePollId,
    LocalDate startDate,
    LocalDate endDate
) {
}
