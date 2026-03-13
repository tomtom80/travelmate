package de.evia.travelmate.iam.application.command;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;

public record CompleteRegistrationCommand(String tokenValue, String password) {

    public CompleteRegistrationCommand {
        argumentIsNotBlank(tokenValue, "tokenValue");
        argumentIsNotBlank(password, "password");
    }
}
