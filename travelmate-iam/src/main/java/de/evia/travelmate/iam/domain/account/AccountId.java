package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        argumentIsNotNull(value, "accountId");
    }
}
