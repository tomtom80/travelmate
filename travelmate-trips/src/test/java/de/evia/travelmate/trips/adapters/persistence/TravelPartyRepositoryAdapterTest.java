package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
        party.addMember(memberId, "max@example.com", "Max", "Mustermann", LocalDate.of(1988, 2, 4));
        party.addDependent(UUID.randomUUID(), memberId, "Lena", "Mustermann", LocalDate.of(2021, 9, 14));

        repository.save(party);

        final TravelParty found = repository.findByTenantId(tenantId).orElseThrow();
        assertThat(found.members()).hasSize(1);
        assertThat(found.dependents()).hasSize(1);
        assertThat(found.members().getFirst().email()).isEqualTo("max@example.com");
        assertThat(found.members().getFirst().dateOfBirth()).isEqualTo(LocalDate.of(1988, 2, 4));
        assertThat(found.dependents().getFirst().firstName()).isEqualTo("Lena");
        assertThat(found.dependents().getFirst().dateOfBirth()).isEqualTo(LocalDate.of(2021, 9, 14));
    }

    @Test
    void findsByMemberEmail() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TravelParty party = TravelParty.create(tenantId, "Wanderurlaub");
        party.addMember(UUID.randomUUID(), "hiker@example.com", "Hans", "Wanderer");
        repository.save(party);

        final Optional<TravelParty> found = repository.findByMemberEmail("hiker@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().tenantId()).isEqualTo(tenantId);
    }

    @Test
    void returnsEmptyForUnknownEmail() {
        final Optional<TravelParty> found = repository.findByMemberEmail("nobody@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownTenant() {
        final Optional<TravelParty> found = repository.findByTenantId(new TenantId(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }

    @Test
    void persistsRenamedTravelPartyName() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TravelParty party = TravelParty.create(tenantId, tenantId.value().toString());

        repository.save(party);

        party.updateName("Familie Kressler");
        repository.save(party);

        final TravelParty found = repository.findByTenantId(tenantId).orElseThrow();
        assertThat(found.name()).isEqualTo("Familie Kressler");
    }
}
