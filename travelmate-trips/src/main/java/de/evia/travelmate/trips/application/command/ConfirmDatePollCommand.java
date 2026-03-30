package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record ConfirmDatePollCommand(
    UUID tenantId,
    UUID datePollId,
    UUID confirmedOptionId
) {
}
