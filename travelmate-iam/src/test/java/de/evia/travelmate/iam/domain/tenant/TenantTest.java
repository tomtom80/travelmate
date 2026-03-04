package de.evia.travelmate.iam.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

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
}
