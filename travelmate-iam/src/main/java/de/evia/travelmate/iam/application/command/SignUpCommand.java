package de.evia.travelmate.iam.application.command;

import java.time.LocalDate;

public record SignUpCommand(
    String tenantName,
    String firstName,
    String lastName,
    String email,
    String password,
    LocalDate dateOfBirth
) {
}
