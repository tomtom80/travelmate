package de.evia.travelmate.iam.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record AddDependentCommand(
    UUID tenantId,
    UUID guardianAccountId,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {
}
