package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.UUID;

public record PendingInvitationView(
    UUID invitationId,
    UUID tripId,
    String tripName,
    LocalDate tripStartDate,
    LocalDate tripEndDate,
    String inviterName
) {
}
