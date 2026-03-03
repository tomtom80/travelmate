package de.evia.travelmate.common.domain;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record TenantId(UUID value) {

    public TenantId {
        argumentIsNotNull(value, "tenantId");
    }
}
