package de.evia.travelmate.trips.domain.travelparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;

class TravelPartyTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());

    @Test
    void createsTravelParty() {
        final TravelParty party = TravelParty.create(TENANT_ID, "Hüttenurlaub 2026");

        assertThat(party.tenantId()).isEqualTo(TENANT_ID);
        assertThat(party.name()).isEqualTo("Hüttenurlaub 2026");
        assertThat(party.members()).isEmpty();
        assertThat(party.dependents()).isEmpty();
    }

    @Test
    void addsMember() {
        final TravelParty party = TravelParty.create(TENANT_ID, "Hüttenurlaub 2026");
        final UUID memberId = UUID.randomUUID();

        party.addMember(memberId, "max@example.com", "Max", "Mustermann");

        assertThat(party.members()).hasSize(1);
        final Member member = party.members().getFirst();
        assertThat(member.memberId()).isEqualTo(memberId);
        assertThat(member.email()).isEqualTo("max@example.com");
        assertThat(member.firstName()).isEqualTo("Max");
        assertThat(member.lastName()).isEqualTo("Mustermann");
    }

    @Test
    void rejectsDuplicateMember() {
        final TravelParty party = TravelParty.create(TENANT_ID, "Hüttenurlaub 2026");
        final UUID memberId = UUID.randomUUID();
        party.addMember(memberId, "max@example.com", "Max", "Mustermann");

        assertThatThrownBy(() -> party.addMember(memberId, "max@example.com", "Max", "Mustermann"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addsDependent() {
        final TravelParty party = TravelParty.create(TENANT_ID, "Hüttenurlaub 2026");
        final UUID guardianId = UUID.randomUUID();
        final UUID dependentId = UUID.randomUUID();
        party.addMember(guardianId, "max@example.com", "Max", "Mustermann");

        party.addDependent(dependentId, guardianId, "Lena", "Mustermann");

        assertThat(party.dependents()).hasSize(1);
        final TravelPartyDependent dep = party.dependents().getFirst();
        assertThat(dep.dependentId()).isEqualTo(dependentId);
        assertThat(dep.guardianMemberId()).isEqualTo(guardianId);
        assertThat(dep.firstName()).isEqualTo("Lena");
    }

    @Test
    void hasMemberReturnsTrueForExistingMember() {
        final TravelParty party = TravelParty.create(TENANT_ID, "Hüttenurlaub 2026");
        final UUID memberId = UUID.randomUUID();
        party.addMember(memberId, "max@example.com", "Max", "Mustermann");

        assertThat(party.hasMember(memberId)).isTrue();
        assertThat(party.hasMember(UUID.randomUUID())).isFalse();
    }
}
