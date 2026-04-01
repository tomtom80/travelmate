package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RecordAccommodationBookingFailureCommand(
    UUID tenantId,
    UUID accommodationPollId,
    String note
) {
}
