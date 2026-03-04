package de.evia.travelmate.iam.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@SpringBootTest
@ActiveProfiles("test")
class TenantRepositoryAdapterTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void savesAndFindsById() {
        final Tenant tenant = IamTestFixtures.tenant();
        tenantRepository.save(tenant);

        final Optional<Tenant> found = tenantRepository.findById(tenant.tenantId());

        assertThat(found).isPresent();
        assertThat(found.get().tenantId()).isEqualTo(tenant.tenantId());
        assertThat(found.get().name().value()).isEqualTo("Reisegruppe Alpen");
    }

    @Test
    void findsAll() {
        final Tenant tenant = Tenant.create(new TenantName("FindAll Test"), null);
        tenantRepository.save(tenant);

        final List<Tenant> all = tenantRepository.findAll();

        assertThat(all).isNotEmpty();
    }

    @Test
    void existsByNameReturnsTrue() {
        final Tenant tenant = Tenant.create(new TenantName("Unique Name Check"), null);
        tenantRepository.save(tenant);

        assertThat(tenantRepository.existsByName(new TenantName("Unique Name Check"))).isTrue();
    }

    @Test
    void existsByNameReturnsFalse() {
        assertThat(tenantRepository.existsByName(new TenantName("Nonexistent Tenant XYZ"))).isFalse();
    }
}
