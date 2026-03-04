package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;

public record KeycloakUserId(String value) {

    public KeycloakUserId {
        argumentIsNotBlank(value, "keycloakUserId");
    }
}
