package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RemoveDateOptionCommand(
    UUID tenantId,
    UUID datePollId,
    UUID dateOptionId
) {
}
