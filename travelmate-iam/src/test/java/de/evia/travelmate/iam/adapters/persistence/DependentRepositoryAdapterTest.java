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
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.dependent.DependentId;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@SpringBootTest
@ActiveProfiles("test")
class DependentRepositoryAdapterTest {

    @Autowired
    private DependentRepository dependentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private TenantId tenantId;
    private AccountId guardianId;

    @BeforeEach
    void setUp() {
        final Tenant tenant = Tenant.create(new TenantName("Dependent Test " + UUID.randomUUID()), null);
        tenantRepository.save(tenant);
        tenantId = tenant.tenantId();

        final Account guardian = new Account(
            new AccountId(UUID.randomUUID()), tenantId,
            new KeycloakUserId("kc-guardian-" + UUID.randomUUID()),
            new Username("guardian-" + UUID.randomUUID()),
            new Email("guardian@example.com"),
            new FullName("Guardian", "User")
        );
        accountRepository.save(guardian);
        guardianId = guardian.accountId();
    }

    @Test
    void savesAndFindsById() {
        final Dependent dependent = new Dependent(
            new DependentId(UUID.randomUUID()), tenantId, guardianId,
            new FullName("Child", "User")
        );
        dependentRepository.save(dependent);

        final Optional<Dependent> found = dependentRepository.findById(dependent.dependentId());

        assertThat(found).isPresent();
        assertThat(found.get().fullName().firstName()).isEqualTo("Child");
        assertThat(found.get().guardianAccountId()).isEqualTo(guardianId);
    }

    @Test
    void findsAllByGuardian() {
        final Dependent dep1 = new Dependent(
            new DependentId(UUID.randomUUID()), tenantId, guardianId,
            new FullName("Child1", "User")
        );
        final Dependent dep2 = new Dependent(
            new DependentId(UUID.randomUUID()), tenantId, guardianId,
            new FullName("Child2", "User")
        );
        dependentRepository.save(dep1);
        dependentRepository.save(dep2);

        final List<Dependent> dependents = dependentRepository.findAllByGuardian(guardianId);

        assertThat(dependents).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findsAllByTenantId() {
        final Dependent dependent = new Dependent(
            new DependentId(UUID.randomUUID()), tenantId, guardianId,
            new FullName("TenantChild", "User")
        );
        dependentRepository.save(dependent);

        final List<Dependent> dependents = dependentRepository.findAllByTenantId(tenantId);

        assertThat(dependents).isNotEmpty();
    }
}
