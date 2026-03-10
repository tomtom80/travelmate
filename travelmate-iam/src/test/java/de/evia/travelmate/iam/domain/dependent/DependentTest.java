package de.evia.travelmate.iam.domain.dependent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.DependentRemovedFromTenant;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.FullName;

class DependentTest {

    @Test
    void addCreatesDependentWithEvent() {
        final Dependent dependent = IamTestFixtures.dependent();
        assertThat(dependent.dependentId()).isNotNull();
        assertThat(dependent.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID);
        assertThat(dependent.guardianAccountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID);
        assertThat(dependent.fullName().firstName()).isEqualTo("Lena");
        assertThat(dependent.fullName().lastName()).isEqualTo("Mustermann");
        assertThat(dependent.domainEvents()).hasSize(1);
        assertThat(dependent.domainEvents().getFirst()).isInstanceOf(DependentAddedToTenant.class);

        final DependentAddedToTenant event = (DependentAddedToTenant) dependent.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(event.dependentId()).isEqualTo(dependent.dependentId().value());
        assertThat(event.guardianAccountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID.value());
        assertThat(event.firstName()).isEqualTo("Lena");
    }

    @Test
    void markForRemovalRegistersEvent() {
        final Dependent dependent = IamTestFixtures.dependent();
        dependent.clearDomainEvents();
        dependent.markForRemoval();

        assertThat(dependent.domainEvents()).hasSize(1);
        assertThat(dependent.domainEvents().getFirst()).isInstanceOf(DependentRemovedFromTenant.class);

        final DependentRemovedFromTenant event = (DependentRemovedFromTenant) dependent.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(event.dependentId()).isEqualTo(dependent.dependentId().value());
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void throwsForNullDependentId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Dependent(null, IamTestFixtures.TENANT_ID,
                IamTestFixtures.ACCOUNT_ID, IamTestFixtures.fullName(), null))
            .withMessageContaining("dependentId");
    }

    @Test
    void throwsForNullTenantId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Dependent(new DependentId(java.util.UUID.randomUUID()),
                null, IamTestFixtures.ACCOUNT_ID, IamTestFixtures.fullName(), null))
            .withMessageContaining("tenantId");
    }

    @Test
    void throwsForNullGuardianAccountId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Dependent(new DependentId(java.util.UUID.randomUUID()),
                IamTestFixtures.TENANT_ID, null, new FullName("Lena", "Mustermann"),
                IamTestFixtures.dateOfBirth()))
            .withMessageContaining("guardianAccountId");
    }

    @Test
    void addRequiresDateOfBirth() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Dependent.add(
                IamTestFixtures.TENANT_ID, IamTestFixtures.ACCOUNT_ID,
                new FullName("Lena", "Mustermann"), null))
            .withMessageContaining("dateOfBirth");
    }

    @Test
    void throwsForNullDateOfBirth() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Dependent(new DependentId(java.util.UUID.randomUUID()),
                IamTestFixtures.TENANT_ID, IamTestFixtures.ACCOUNT_ID,
                new FullName("Lena", "Mustermann"), null))
            .withMessageContaining("dateOfBirth");
    }
}
