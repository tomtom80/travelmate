package de.evia.travelmate.iam.domain.tenant;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.events.iam.TenantDeleted;

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
        final Tenant tenant = new Tenant(new TenantId(UUID.randomUUID()), name, description);
        tenant.registerEvent(new TenantCreated(
            tenant.tenantId.value(),
            name.value(),
            LocalDate.now()
        ));
        return tenant;
    }

    public void markForDeletion() {
        registerEvent(new TenantDeleted(
            tenantId.value(),
            LocalDate.now()
        ));
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
