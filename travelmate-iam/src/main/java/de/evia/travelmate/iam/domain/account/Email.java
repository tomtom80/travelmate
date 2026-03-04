package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        argumentIsNotBlank(value, "email");
        argumentIsTrue(EMAIL_PATTERN.matcher(value).matches(),
            "The email must be a valid email address.");
    }
}
