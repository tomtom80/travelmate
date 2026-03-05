package de.evia.travelmate.iam.application.command;

public record SignUpCommand(
    String tenantName,
    String firstName,
    String lastName,
    String email,
    String password
) {
}
