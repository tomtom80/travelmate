package de.evia.travelmate.iam.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record InviteMemberCommand(
    UUID tenantId,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {
}
