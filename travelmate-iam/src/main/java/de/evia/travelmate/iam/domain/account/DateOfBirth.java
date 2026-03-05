package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;

public record DateOfBirth(LocalDate value) {

    public DateOfBirth {
        argumentIsNotNull(value, "dateOfBirth");
    }
}
