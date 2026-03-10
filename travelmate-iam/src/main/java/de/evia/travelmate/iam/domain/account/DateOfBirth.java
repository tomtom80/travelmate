package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.time.LocalDate;

public record DateOfBirth(LocalDate value) {

    public DateOfBirth {
        argumentIsNotNull(value, "dateOfBirth");
        argumentIsTrue(!value.isAfter(LocalDate.now()),
            "Date of birth must not be in the future.");
        argumentIsTrue(!value.isBefore(LocalDate.now().minusYears(150)),
            "Date of birth must not be more than 150 years ago.");
    }
}
