package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.UUID;

public record ParticipantView(
    UUID participantId,
    String firstName,
    String lastName,
    String partyName,
    LocalDate arrivalDate,
    LocalDate departureDate,
    boolean manageableByCurrentParty,
    boolean organizerEligible
) {
}
