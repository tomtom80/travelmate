package de.evia.travelmate.iam.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.events.iam.TenantDeleted;
import de.evia.travelmate.iam.domain.IamTestFixtures;

class TenantTest {

    @Test
    void createsWithAllFields() {
        final Tenant tenant = IamTestFixtures.tenant();
        assertThat(tenant.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID);
        assertThat(tenant.name()).isEqualTo(IamTestFixtures.tenantName());
        assertThat(tenant.description()).isEqualTo(IamTestFixtures.description());
    }

    @Test
    void createGeneratesNewId() {
        final Tenant tenant = Tenant.create(IamTestFixtures.tenantName(), IamTestFixtures.description());
        assertThat(tenant.tenantId()).isNotNull();
        assertThat(tenant.tenantId().value()).isNotNull();
        assertThat(tenant.name()).isEqualTo(IamTestFixtures.tenantName());
    }

    @Test
    void createRegistersTenantCreatedEvent() {
        final Tenant tenant = Tenant.create(IamTestFixtures.tenantName(), IamTestFixtures.description());
        assertThat(tenant.domainEvents()).hasSize(1);
        assertThat(tenant.domainEvents().getFirst()).isInstanceOf(TenantCreated.class);

        final TenantCreated event = (TenantCreated) tenant.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(tenant.tenantId().value());
        assertThat(event.tenantName()).isEqualTo(IamTestFixtures.tenantName().value());
    }

    @Test
    void throwsForNullTenantId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Tenant(null, IamTestFixtures.tenantName(), IamTestFixtures.description()))
            .withMessageContaining("tenantId");
    }

    @Test
    void throwsForNullName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Tenant(IamTestFixtures.TENANT_ID, null, IamTestFixtures.description()))
            .withMessageContaining("tenantName");
    }

    @Test
    void allowsNullDescription() {
        final Tenant tenant = new Tenant(IamTestFixtures.TENANT_ID, IamTestFixtures.tenantName(), null);
        assertThat(tenant.description()).isNull();
    }

    @Test
    void markForDeletionRegistersEvent() {
        final Tenant tenant = IamTestFixtures.tenant();
        tenant.markForDeletion();

        assertThat(tenant.domainEvents()).hasSize(1);
        assertThat(tenant.domainEvents().getFirst()).isInstanceOf(TenantDeleted.class);

        final TenantDeleted event = (TenantDeleted) tenant.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(event.occurredOn()).isNotNull();
    }
}
