package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;

public record FullName(String firstName, String lastName) {

    public FullName {
        argumentIsNotBlank(firstName, "firstName");
        argumentIsNotBlank(lastName, "lastName");
    }
}
