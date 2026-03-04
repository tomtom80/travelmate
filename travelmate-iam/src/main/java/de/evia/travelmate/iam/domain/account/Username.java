package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;

public record Username(String value) {

    public Username {
        argumentIsNotBlank(value, "username");
    }
}
