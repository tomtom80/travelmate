package de.evia.travelmate.trips.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record InviteExternalCommand(
    UUID tenantId,
    UUID tripId,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    UUID invitedBy
) {
}
