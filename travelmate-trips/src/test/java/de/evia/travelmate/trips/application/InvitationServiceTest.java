package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.domain.invitation.Invitation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.invitation.InvitationRepository;
import de.evia.travelmate.trips.domain.invitation.InvitationStatus;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final TenantId TENANT_ID = new TenantId(TENANT_UUID);
    private static final UUID ORGANIZER_ID = UUID.randomUUID();
    private static final UUID INVITEE_ID = UUID.randomUUID();

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelPartyRepository travelPartyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void inviteCreatesInvitationForTravelPartyMember() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeId(trip.tripId(), INVITEE_ID)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        final InvitationRepresentation result = invitationService.invite(
            new InviteParticipantCommand(TENANT_UUID, trip.tripId().value(), INVITEE_ID, ORGANIZER_ID)
        );

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.inviteeId()).isEqualTo(INVITEE_ID);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void inviteRejectsDuplicateInvitation() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeId(trip.tripId(), INVITEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.invite(
            new InviteParticipantCommand(TENANT_UUID, trip.tripId().value(), INVITEE_ID, ORGANIZER_ID)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inviteRejectsNonMemberInvitee() {
        final Trip trip = createTrip();
        final UUID nonMemberId = UUID.randomUUID();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));

        assertThatThrownBy(() -> invitationService.invite(
            new InviteParticipantCommand(TENANT_UUID, trip.tripId().value(), nonMemberId, ORGANIZER_ID)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptAddsParticipantToTripAndPublishesEvent() {
        final Trip trip = createTrip();
        final Invitation invitation = Invitation.create(TENANT_ID, trip.tripId(), INVITEE_ID, ORGANIZER_ID);
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));

        invitationService.accept(invitation.invitationId());

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(trip.hasParticipant(INVITEE_ID)).isTrue();
        verify(tripRepository).save(trip);
        verify(invitationRepository).save(invitation);

        final ArgumentCaptor<ParticipantJoinedTrip> eventCaptor =
            ArgumentCaptor.forClass(ParticipantJoinedTrip.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final ParticipantJoinedTrip event = eventCaptor.getValue();
        assertThat(event.participantId()).isEqualTo(INVITEE_ID);
        assertThat(event.tripId()).isEqualTo(trip.tripId().value());
    }

    @Test
    void declineSetsStatusToDeclined() {
        final Invitation invitation = Invitation.create(TENANT_ID, new TripId(UUID.randomUUID()), INVITEE_ID, ORGANIZER_ID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));

        invitationService.decline(invitation.invitationId());

        assertThat(invitation.status()).isEqualTo(InvitationStatus.DECLINED);
        verify(invitationRepository).save(invitation);
    }

    private Trip createTrip() {
        return Trip.plan(
            TENANT_ID, new TripName("Skiurlaub"),
            null, new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID
        );
    }

    private TravelParty createPartyWithMembers(final UUID... memberIds) {
        final TravelParty party = TravelParty.create(TENANT_ID, "Familie Test");
        for (final UUID memberId : memberIds) {
            party.addMember(memberId, memberId + "@test.de", "Test", "User");
        }
        return party;
    }
}
