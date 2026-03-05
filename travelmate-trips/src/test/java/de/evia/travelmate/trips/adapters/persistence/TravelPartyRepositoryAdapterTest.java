package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@SpringBootTest
@ActiveProfiles("test")
class TravelPartyRepositoryAdapterTest {

    @Autowired
    private TravelPartyRepository repository;

    @Test
    void savesAndFindsTravelParty() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TravelParty party = TravelParty.create(tenantId, "Hüttenurlaub 2026");

        repository.save(party);

        final Optional<TravelParty> found = repository.findByTenantId(tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Hüttenurlaub 2026");
    }

    @Test
    void savesWithMembersAndDependents() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TravelParty party = TravelParty.create(tenantId, "Skiurlaub");
        final UUID memberId = UUID.randomUUID();
        party.addMember(memberId, "max@example.com", "Max", "Mustermann");
        party.addDependent(UUID.randomUUID(), memberId, "Lena", "Mustermann");

        repository.save(party);

        final TravelParty found = repository.findByTenantId(tenantId).orElseThrow();
        assertThat(found.members()).hasSize(1);
        assertThat(found.dependents()).hasSize(1);
        assertThat(found.members().getFirst().email()).isEqualTo("max@example.com");
        assertThat(found.dependents().getFirst().firstName()).isEqualTo("Lena");
    }

    @Test
    void returnsEmptyForUnknownTenant() {
        final Optional<TravelParty> found = repository.findByTenantId(new TenantId(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }
}
