package de.evia.travelmate.iam.application.representation;

import java.util.UUID;

import de.evia.travelmate.iam.domain.tenant.Tenant;

public record TenantRepresentation(UUID tenantId, String name, String description) {

    public TenantRepresentation(final Tenant tenant) {
        this(
            tenant.tenantId().value(),
            tenant.name().value(),
            tenant.description() != null ? tenant.description().value() : null
        );
    }
}
