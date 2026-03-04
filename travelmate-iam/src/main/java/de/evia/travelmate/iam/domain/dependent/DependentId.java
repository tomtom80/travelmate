package de.evia.travelmate.iam.domain.dependent;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record DependentId(UUID value) {

    public DependentId {
        argumentIsNotNull(value, "dependentId");
    }
}
