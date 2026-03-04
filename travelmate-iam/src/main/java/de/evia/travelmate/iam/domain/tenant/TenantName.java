package de.evia.travelmate.iam.domain.tenant;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;

public record TenantName(String value) {

    public TenantName {
        argumentIsNotBlank(value, "tenantName");
    }
}
