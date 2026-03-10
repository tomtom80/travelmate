package de.evia.travelmate.iam.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterAccountCommand(
    UUID tenantId,
    String keycloakUserId,
    String username,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {
}
