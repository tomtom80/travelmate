package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record Password(String value) {

    private static final int MIN_LENGTH = 8;

    public Password {
        argumentIsNotBlank(value, "password");
        argumentIsTrue(value.length() >= MIN_LENGTH,
            "The password must be at least " + MIN_LENGTH + " characters long.");
    }

    @Override
    public String toString() {
        return "Password[***]";
    }
}
