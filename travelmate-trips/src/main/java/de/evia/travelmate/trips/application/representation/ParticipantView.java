package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.UUID;

public record ParticipantView(
    UUID participantId,
    String firstName,
    String lastName,
    LocalDate arrivalDate,
    LocalDate departureDate
) {
}
