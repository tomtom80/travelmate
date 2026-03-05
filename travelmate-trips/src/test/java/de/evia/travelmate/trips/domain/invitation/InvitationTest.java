package de.evia.travelmate.trips.domain.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

class InvitationTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final UUID INVITEE_ID = UUID.randomUUID();
    private static final UUID INVITED_BY = UUID.randomUUID();

    @Test
    void createInitializesInvitationAsPending() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);

        assertThat(invitation.invitationId()).isNotNull();
        assertThat(invitation.tenantId()).isEqualTo(TENANT_ID);
        assertThat(invitation.tripId()).isEqualTo(TRIP_ID);
        assertThat(invitation.inviteeId()).isEqualTo(INVITEE_ID);
        assertThat(invitation.invitedBy()).isEqualTo(INVITED_BY);
        assertThat(invitation.status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void acceptTransitionsToPendingToAccepted() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);

        invitation.accept();

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void declineTransitionsPendingToDeclined() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);

        invitation.decline();

        assertThat(invitation.status()).isEqualTo(InvitationStatus.DECLINED);
    }

    @Test
    void cannotAcceptAlreadyAcceptedInvitation() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);
        invitation.accept();

        assertThatThrownBy(invitation::accept)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotDeclineAlreadyAcceptedInvitation() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);
        invitation.accept();

        assertThatThrownBy(invitation::decline)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotAcceptDeclinedInvitation() {
        final Invitation invitation = Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, INVITED_BY);
        invitation.decline();

        assertThatThrownBy(invitation::accept)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNullTenantId() {
        assertThatThrownBy(() -> Invitation.create(null, TRIP_ID, INVITEE_ID, INVITED_BY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullTripId() {
        assertThatThrownBy(() -> Invitation.create(TENANT_ID, null, INVITEE_ID, INVITED_BY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullInviteeId() {
        assertThatThrownBy(() -> Invitation.create(TENANT_ID, TRIP_ID, null, INVITED_BY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullInvitedBy() {
        assertThatThrownBy(() -> Invitation.create(TENANT_ID, TRIP_ID, INVITEE_ID, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
