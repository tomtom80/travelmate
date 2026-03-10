package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.MemberRemovedFromTenant;
import de.evia.travelmate.iam.domain.IamTestFixtures;

class AccountTest {

    @Test
    void createsWithAllFields() {
        final Account account = IamTestFixtures.account();
        assertThat(account.accountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID);
        assertThat(account.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID);
        assertThat(account.username().value()).isEqualTo("testuser");
        assertThat(account.email().value()).isEqualTo("test@example.com");
        assertThat(account.fullName().firstName()).isEqualTo("Max");
        assertThat(account.fullName().lastName()).isEqualTo("Mustermann");
    }

    @Test
    void registerCreatesAccountWithEvent() {
        final Account account = IamTestFixtures.registeredAccount();
        assertThat(account.accountId()).isNotNull();
        assertThat(account.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID);
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().getFirst()).isInstanceOf(AccountRegistered.class);

        final AccountRegistered event = (AccountRegistered) account.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(event.accountId()).isEqualTo(account.accountId().value());
        assertThat(event.username()).isEqualTo(account.username().value());
        assertThat(event.email()).isEqualTo(account.email().value());
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void markForRemovalRegistersEvent() {
        final Account account = IamTestFixtures.account();
        account.markForRemoval();

        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().getFirst()).isInstanceOf(MemberRemovedFromTenant.class);

        final MemberRemovedFromTenant event = (MemberRemovedFromTenant) account.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(IamTestFixtures.TENANT_ID.value());
        assertThat(event.accountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID.value());
        assertThat(event.email()).isEqualTo("test@example.com");
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void throwsForNullAccountId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Account(null, IamTestFixtures.TENANT_ID,
                IamTestFixtures.keycloakUserId(), IamTestFixtures.username(),
                IamTestFixtures.email(), IamTestFixtures.fullName(), null))
            .withMessageContaining("accountId");
    }

    @Test
    void throwsForNullTenantId() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Account(IamTestFixtures.ACCOUNT_ID, null,
                IamTestFixtures.keycloakUserId(), IamTestFixtures.username(),
                IamTestFixtures.email(), IamTestFixtures.fullName(), null))
            .withMessageContaining("tenantId");
    }

    @Test
    void throwsForNullUsername() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Account(IamTestFixtures.ACCOUNT_ID, IamTestFixtures.TENANT_ID,
                IamTestFixtures.keycloakUserId(), null,
                IamTestFixtures.email(), IamTestFixtures.fullName(), IamTestFixtures.dateOfBirth()))
            .withMessageContaining("username");
    }

    @Test
    void registerRequiresDateOfBirth() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Account.register(
                IamTestFixtures.TENANT_ID, IamTestFixtures.keycloakUserId(),
                IamTestFixtures.username(), IamTestFixtures.email(),
                IamTestFixtures.fullName(), null))
            .withMessageContaining("dateOfBirth");
    }

    @Test
    void throwsForNullDateOfBirth() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Account(IamTestFixtures.ACCOUNT_ID, IamTestFixtures.TENANT_ID,
                IamTestFixtures.keycloakUserId(), IamTestFixtures.username(),
                IamTestFixtures.email(), IamTestFixtures.fullName(), null))
            .withMessageContaining("dateOfBirth");
    }
}
