package de.evia.travelmate.iam.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@SpringBootTest
@ActiveProfiles("test")
class AccountRepositoryAdapterTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        final Tenant tenant = Tenant.create(new TenantName("Account Test Tenant " + UUID.randomUUID()), null);
        tenantRepository.save(tenant);
        tenantId = tenant.tenantId();
    }

    @Test
    void savesAndFindsById() {
        final Account account = createAccount("user1", "kc-1");
        accountRepository.save(account);

        final Optional<Account> found = accountRepository.findById(account.accountId());

        assertThat(found).isPresent();
        assertThat(found.get().username().value()).isEqualTo("user1");
        assertThat(found.get().email().value()).isEqualTo("user1@example.com");
    }

    @Test
    void findsByKeycloakUserId() {
        final Account account = createAccount("user2", "kc-find-by-kc");
        accountRepository.save(account);

        final Optional<Account> found = accountRepository.findByKeycloakUserId(
            tenantId, new KeycloakUserId("kc-find-by-kc"));

        assertThat(found).isPresent();
        assertThat(found.get().username().value()).isEqualTo("user2");
    }

    @Test
    void findsAllByTenantId() {
        final Account account = createAccount("user3", "kc-3");
        accountRepository.save(account);

        final List<Account> accounts = accountRepository.findAllByTenantId(tenantId);

        assertThat(accounts).isNotEmpty();
    }

    @Test
    void existsByUsernameReturnsTrue() {
        final Account account = createAccount("existsuser", "kc-exists");
        accountRepository.save(account);

        assertThat(accountRepository.existsByUsername(tenantId, new Username("existsuser"))).isTrue();
    }

    @Test
    void existsByUsernameReturnsFalse() {
        assertThat(accountRepository.existsByUsername(tenantId, new Username("nonexistent"))).isFalse();
    }

    private Account createAccount(final String username, final String keycloakId) {
        return new Account(
            new AccountId(UUID.randomUUID()),
            tenantId,
            new KeycloakUserId(keycloakId),
            new Username(username),
            new Email(username + "@example.com"),
            new FullName("Test", "User")
        );
    }
}
