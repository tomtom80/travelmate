package de.evia.travelmate.iam.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.DateOfBirth;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    private static final TenantId TENANT_A = new TenantId(UUID.randomUUID());
    private static final TenantId TENANT_B = new TenantId(UUID.randomUUID());

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void accountsFromTenantANotVisibleToTenantB() {
        final Account accountA = Account.register(
            TENANT_A,
            new KeycloakUserId("kc-" + UUID.randomUUID()),
            new Username("alice-" + UUID.randomUUID().toString().substring(0, 8)),
            new Email("alice-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
            new FullName("Alice", "Smith"),
            new DateOfBirth(LocalDate.of(1990, 1, 1))
        );
        accountRepository.save(accountA);

        final List<Account> tenantAAccounts = accountRepository.findAllByTenantId(TENANT_A);
        final List<Account> tenantBAccounts = accountRepository.findAllByTenantId(TENANT_B);

        assertThat(tenantAAccounts).extracting(a -> a.accountId().value())
            .contains(accountA.accountId().value());
        assertThat(tenantBAccounts).extracting(a -> a.accountId().value())
            .doesNotContain(accountA.accountId().value());
    }

    @Test
    void findByKeycloakUserIdWithTenantScopeRespectsIsolation() {
        final String keycloakId = "kc-" + UUID.randomUUID();
        final Account accountA = Account.register(
            TENANT_A,
            new KeycloakUserId(keycloakId),
            new Username("bob-" + UUID.randomUUID().toString().substring(0, 8)),
            new Email("bob-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
            new FullName("Bob", "Jones"),
            new DateOfBirth(LocalDate.of(1985, 6, 15))
        );
        accountRepository.save(accountA);

        final Optional<Account> foundByTenantA = accountRepository.findByKeycloakUserId(
            TENANT_A, new KeycloakUserId(keycloakId));
        final Optional<Account> foundByTenantB = accountRepository.findByKeycloakUserId(
            TENANT_B, new KeycloakUserId(keycloakId));

        assertThat(foundByTenantA).isPresent();
        assertThat(foundByTenantB).isEmpty();
    }

    @Test
    void existsByUsernameRespectsTenanScope() {
        final String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        final Username username = new Username("charlie-" + uniqueSuffix);
        final Account accountA = Account.register(
            TENANT_A,
            new KeycloakUserId("kc-" + UUID.randomUUID()),
            username,
            new Email("charlie-" + uniqueSuffix + "@example.com"),
            new FullName("Charlie", "Brown"),
            new DateOfBirth(LocalDate.of(1992, 3, 20))
        );
        accountRepository.save(accountA);

        assertThat(accountRepository.existsByUsername(TENANT_A, username)).isTrue();
        assertThat(accountRepository.existsByUsername(TENANT_B, username)).isFalse();
    }

    @Test
    void countByTenantIdOnlyCountsOwnTenant() {
        final long beforeA = accountRepository.countByTenantId(TENANT_A);
        final long beforeB = accountRepository.countByTenantId(TENANT_B);

        final Account accountA = Account.register(
            TENANT_A,
            new KeycloakUserId("kc-" + UUID.randomUUID()),
            new Username("dave-" + UUID.randomUUID().toString().substring(0, 8)),
            new Email("dave-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
            new FullName("Dave", "Wilson"),
            new DateOfBirth(LocalDate.of(1988, 11, 5))
        );
        accountRepository.save(accountA);

        assertThat(accountRepository.countByTenantId(TENANT_A)).isEqualTo(beforeA + 1);
        assertThat(accountRepository.countByTenantId(TENANT_B)).isEqualTo(beforeB);
    }
}
