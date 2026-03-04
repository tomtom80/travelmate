package de.evia.travelmate.iam.domain.tenant;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;

public class Tenant extends AggregateRoot {

    private final TenantId tenantId;
    private final TenantName name;
    private final Description description;

    public Tenant(final TenantId tenantId, final TenantName name, final Description description) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(name, "tenantName");
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
    }

    public static Tenant create(final TenantName name, final Description description) {
        return new Tenant(new TenantId(java.util.UUID.randomUUID()), name, description);
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TenantName name() {
        return name;
    }

    public Description description() {
        return description;
    }
}
