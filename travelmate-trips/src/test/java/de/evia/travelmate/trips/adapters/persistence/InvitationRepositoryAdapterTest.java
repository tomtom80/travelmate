package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.invitation.Invitation;
import de.evia.travelmate.trips.domain.invitation.InvitationRepository;
import de.evia.travelmate.trips.domain.invitation.InvitationStatus;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class InvitationRepositoryAdapterTest {

    @Autowired
    private InvitationRepository repository;

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());

    @Test
    void savesAndFindsMemberInvitation() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final UUID inviteeId = UUID.randomUUID();
        final UUID invitedBy = UUID.randomUUID();
        final UUID targetPartyTenantId = UUID.randomUUID();
        final Invitation invitation = Invitation.create(TENANT_ID, tripId, inviteeId, invitedBy, targetPartyTenantId);

        repository.save(invitation);

        final Optional<Invitation> found = repository.findById(invitation.invitationId());
        assertThat(found).isPresent();
        assertThat(found.get().tenantId()).isEqualTo(TENANT_ID);
        assertThat(found.get().tripId()).isEqualTo(tripId);
        assertThat(found.get().inviteeId()).isEqualTo(inviteeId);
        assertThat(found.get().targetPartyTenantId()).isEqualTo(targetPartyTenantId);
        assertThat(found.get().status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void savesAndFindsExternalInvitation() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final UUID invitedBy = UUID.randomUUID();
        final Invitation invitation = Invitation.inviteExternal(TENANT_ID, tripId, "ext@test.de", invitedBy);

        repository.save(invitation);

        final Optional<Invitation> found = repository.findById(invitation.invitationId());
        assertThat(found).isPresent();
        assertThat(found.get().inviteeId()).isNull();
        assertThat(found.get().inviteeEmail()).isEqualTo("ext@test.de");
        assertThat(found.get().status()).isEqualTo(InvitationStatus.AWAITING_REGISTRATION);
    }

    @Test
    void findsByTripId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final UUID invitedBy = UUID.randomUUID();
        repository.save(Invitation.create(TENANT_ID, tripId, UUID.randomUUID(), invitedBy, UUID.randomUUID()));
        repository.save(Invitation.create(TENANT_ID, tripId, UUID.randomUUID(), invitedBy, UUID.randomUUID()));

        final List<Invitation> found = repository.findByTripId(tripId);

        assertThat(found).hasSize(2);
    }

    @Test
    void findsByInviteeIdAndStatus() {
        final UUID inviteeId = UUID.randomUUID();
        final UUID invitedBy = UUID.randomUUID();
        final Invitation pending = Invitation.create(TENANT_ID, new TripId(UUID.randomUUID()), inviteeId, invitedBy, UUID.randomUUID());
        final Invitation accepted = Invitation.create(TENANT_ID, new TripId(UUID.randomUUID()), inviteeId, invitedBy, UUID.randomUUID());
        accepted.accept();
        repository.save(pending);
        repository.save(accepted);

        final List<Invitation> pendingOnly = repository.findByInviteeIdAndStatus(inviteeId, InvitationStatus.PENDING);

        assertThat(pendingOnly).hasSize(1);
        assertThat(pendingOnly.getFirst().status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void findsByInviteeEmailAndStatus() {
        final String email = "awaiting-" + UUID.randomUUID() + "@test.de";
        final UUID invitedBy = UUID.randomUUID();
        final Invitation awaiting = Invitation.inviteExternal(TENANT_ID, new TripId(UUID.randomUUID()), email, invitedBy);
        repository.save(awaiting);

        final List<Invitation> found = repository.findByInviteeEmailAndStatus(email, InvitationStatus.AWAITING_REGISTRATION);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().inviteeEmail()).isEqualTo(email);
    }

    @Test
    void existsByTripIdAndInviteeId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final UUID inviteeId = UUID.randomUUID();
        repository.save(Invitation.create(TENANT_ID, tripId, inviteeId, UUID.randomUUID(), UUID.randomUUID()));

        assertThat(repository.existsByTripIdAndInviteeId(tripId, inviteeId)).isTrue();
        assertThat(repository.existsByTripIdAndInviteeId(tripId, UUID.randomUUID())).isFalse();
    }

    @Test
    void existsByTripIdAndInviteeEmail() {
        final TripId tripId = new TripId(UUID.randomUUID());
        repository.save(Invitation.inviteExternal(TENANT_ID, tripId, "check@test.de", UUID.randomUUID()));

        assertThat(repository.existsByTripIdAndInviteeEmail(tripId, "check@test.de")).isTrue();
        assertThat(repository.existsByTripIdAndInviteeEmail(tripId, "other@test.de")).isFalse();
    }

    @Test
    void updatesStatusOnSave() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Invitation invitation = Invitation.create(TENANT_ID, tripId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        repository.save(invitation);

        invitation.accept();
        repository.save(invitation);

        final Invitation found = repository.findById(invitation.invitationId()).orElseThrow();
        assertThat(found.status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void linkToMemberUpdatesInviteeId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Invitation invitation = Invitation.inviteExternal(TENANT_ID, tripId, "link@test.de", UUID.randomUUID());
        repository.save(invitation);

        final UUID memberId = UUID.randomUUID();
        invitation.linkToMember(memberId, UUID.randomUUID());
        repository.save(invitation);

        final Invitation found = repository.findById(invitation.invitationId()).orElseThrow();
        assertThat(found.inviteeId()).isEqualTo(memberId);
        assertThat(found.status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void returnsEmptyForUnknownId() {
        final Optional<Invitation> found = repository.findById(new de.evia.travelmate.trips.domain.invitation.InvitationId(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }

    @Test
    void existsByTripIdAndTargetPartyTenantIdInStatuses() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final UUID targetPartyTenantId = UUID.randomUUID();
        repository.save(Invitation.create(TENANT_ID, tripId, UUID.randomUUID(), UUID.randomUUID(), targetPartyTenantId));

        assertThat(repository.existsByTripIdAndTargetPartyTenantIdInStatuses(
            tripId,
            targetPartyTenantId,
            List.of(InvitationStatus.PENDING))).isTrue();
    }
}
