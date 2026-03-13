package de.evia.travelmate.iam.application.command;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;

public record RegisterExternalUserCommand(
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {

    public RegisterExternalUserCommand {
        argumentIsNotBlank(email, "email");
        argumentIsNotBlank(firstName, "firstName");
        argumentIsNotBlank(lastName, "lastName");
        argumentIsNotNull(dateOfBirth, "dateOfBirth");
    }
}
