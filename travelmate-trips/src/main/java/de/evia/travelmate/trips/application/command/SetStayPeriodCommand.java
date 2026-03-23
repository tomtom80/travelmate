package de.evia.travelmate.trips.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record SetStayPeriodCommand(
    UUID tripId,
    UUID participantId,
    UUID actorId,
    UUID actorPartyTenantId,
    LocalDate arrivalDate,
    LocalDate departureDate
) {
}
