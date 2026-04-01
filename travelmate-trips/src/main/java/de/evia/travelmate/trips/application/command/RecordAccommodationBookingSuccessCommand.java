package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RecordAccommodationBookingSuccessCommand(
    UUID tenantId,
    UUID tripId,
    UUID accommodationPollId
) {
}
