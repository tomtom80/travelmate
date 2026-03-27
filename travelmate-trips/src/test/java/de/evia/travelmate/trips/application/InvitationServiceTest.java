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

import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;
import de.evia.travelmate.common.events.trips.InvitationCreated;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.trips.application.command.InviteExternalCommand;
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

    @Mock
    private TripParticipationEventPublisher tripParticipationEventPublisher;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void inviteCreatesInvitationForTravelPartyMember() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeId(trip.tripId(), INVITEE_ID)).thenReturn(false);
        when(invitationRepository.existsByTripIdAndTargetPartyTenantIdInStatuses(
            trip.tripId(), TENANT_UUID, List.of(InvitationStatus.PENDING, InvitationStatus.AWAITING_REGISTRATION)))
            .thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        final InvitationRepresentation result = invitationService.invite(
            new InviteParticipantCommand(TENANT_UUID, trip.tripId().value(), INVITEE_ID, ORGANIZER_ID)
        );

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.inviteeId()).isEqualTo(INVITEE_ID);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void invitePublishesInvitationCreatedEvent() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeId(trip.tripId(), INVITEE_ID)).thenReturn(false);
        when(invitationRepository.existsByTripIdAndTargetPartyTenantIdInStatuses(
            trip.tripId(), TENANT_UUID, List.of(InvitationStatus.PENDING, InvitationStatus.AWAITING_REGISTRATION)))
            .thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        invitationService.invite(
            new InviteParticipantCommand(TENANT_UUID, trip.tripId().value(), INVITEE_ID, ORGANIZER_ID)
        );

        final ArgumentCaptor<InvitationCreated> eventCaptor =
            ArgumentCaptor.forClass(InvitationCreated.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final InvitationCreated event = eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_UUID);
        assertThat(event.tripId()).isEqualTo(trip.tripId().value());
        assertThat(event.tripName()).isEqualTo("Skiurlaub");
        assertThat(event.inviteeEmail()).isEqualTo(INVITEE_ID + "@test.de");
        assertThat(event.inviteeFirstName()).isEqualTo("Test");
        assertThat(event.inviterFirstName()).isEqualTo("Test");
        assertThat(event.inviterLastName()).isEqualTo("User");
        assertThat(event.tripStartDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(event.tripEndDate()).isEqualTo(LocalDate.of(2026, 3, 22));
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
        )).isInstanceOf(de.evia.travelmate.common.domain.DuplicateEntityException.class);
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
        )).isInstanceOf(de.evia.travelmate.common.domain.EntityNotFoundException.class);
    }

    @Test
    void acceptAddsParticipantToTripAndPublishesEvent() {
        final Trip trip = createTrip();
        final Invitation invitation = Invitation.create(TENANT_ID, trip.tripId(), INVITEE_ID, ORGANIZER_ID, TENANT_UUID);
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));

        invitationService.accept(invitation.invitationId(), INVITEE_ID);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(trip.hasParticipant(INVITEE_ID)).isTrue();
        verify(tripRepository).save(trip);
        verify(invitationRepository).save(invitation);

        final ArgumentCaptor<ParticipantJoinedTrip> eventCaptor =
            ArgumentCaptor.forClass(ParticipantJoinedTrip.class);
        verify(tripParticipationEventPublisher).publishParticipantJoinedAfterCommit(eventCaptor.capture());
        final ParticipantJoinedTrip event = eventCaptor.getValue();
        assertThat(event.participantId()).isEqualTo(INVITEE_ID);
        assertThat(event.tripId()).isEqualTo(trip.tripId().value());
    }

    @Test
    void acceptResolvesInvitedMemberFromTargetPartyTenant() {
        final Trip trip = createTrip();
        final UUID targetPartyTenantId = UUID.randomUUID();
        final Invitation invitation = Invitation.create(TENANT_ID, trip.tripId(), INVITEE_ID, ORGANIZER_ID, targetPartyTenantId);
        final TravelParty targetParty = TravelParty.create(new TenantId(targetPartyTenantId), "Familie Ziel");
        targetParty.addMember(INVITEE_ID, "invitee@test.de", "Rita", "Receiver");

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(new TenantId(targetPartyTenantId))).thenReturn(Optional.of(targetParty));

        invitationService.accept(invitation.invitationId(), INVITEE_ID);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(trip.hasParticipant(INVITEE_ID)).isTrue();
        verify(tripRepository).save(trip);
        verify(invitationRepository).save(invitation);
    }

    @Test
    void acceptRejectsAlreadyExistingParticipant() {
        final Trip trip = createTrip();
        trip.addParticipant(INVITEE_ID, "Test", "User");
        final Invitation invitation = Invitation.create(TENANT_ID, trip.tripId(), INVITEE_ID, ORGANIZER_ID, TENANT_UUID);
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID, INVITEE_ID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));

        assertThatThrownBy(() -> invitationService.accept(invitation.invitationId(), INVITEE_ID))
            .isInstanceOf(DuplicateEntityException.class)
            .hasMessageContaining("participant.error.alreadyExists");
    }

    @Test
    void declineSetsStatusToDeclined() {
        final Invitation invitation = Invitation.create(TENANT_ID, new TripId(UUID.randomUUID()), INVITEE_ID, ORGANIZER_ID, TENANT_UUID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));

        invitationService.decline(invitation.invitationId(), INVITEE_ID);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.DECLINED);
        verify(invitationRepository).save(invitation);
    }

    @Test
    void acceptRejectsDifferentActor() {
        final Trip trip = createTrip();
        final Invitation invitation = Invitation.create(TENANT_ID, trip.tripId(), INVITEE_ID, ORGANIZER_ID, TENANT_UUID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.accept(invitation.invitationId(), ORGANIZER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invited member");
    }

    @Test
    void declineRejectsDifferentActor() {
        final Invitation invitation = Invitation.create(TENANT_ID, new TripId(UUID.randomUUID()), INVITEE_ID, ORGANIZER_ID, TENANT_UUID);

        when(invitationRepository.findById(invitation.invitationId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.decline(invitation.invitationId(), ORGANIZER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invited member");
    }

    @Test
    void inviteExternalCreatesAwaitingRegistrationInvitation() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeEmail(trip.tripId(), "new@test.de")).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        final InvitationRepresentation result = invitationService.inviteExternal(
            new InviteExternalCommand(TENANT_UUID, trip.tripId().value(), "new@test.de",
                "New", "User", LocalDate.of(1990, 1, 1), ORGANIZER_ID)
        );

        assertThat(result.status()).isEqualTo("AWAITING_REGISTRATION");
        assertThat(result.inviteeEmail()).isEqualTo("new@test.de");
        assertThat(result.invitationType()).isEqualTo("EXTERNAL");
    }

    @Test
    void inviteExternalPublishesBothEvents() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeEmail(trip.tripId(), "new@test.de")).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        invitationService.inviteExternal(
            new InviteExternalCommand(TENANT_UUID, trip.tripId().value(), "new@test.de",
                "New", "User", LocalDate.of(1990, 1, 1), ORGANIZER_ID)
        );

        final ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
        final List<Object> events = eventCaptor.getAllValues();
        assertThat(events.get(0)).isInstanceOf(InvitationCreated.class);
        assertThat(events.get(1)).isInstanceOf(ExternalUserInvitedToTrip.class);
    }

    @Test
    void inviteExternalRejectsDuplicateEmail() {
        final Trip trip = createTrip();
        final TravelParty party = createPartyWithMembers(ORGANIZER_ID);

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(party));
        when(invitationRepository.existsByTripIdAndInviteeEmail(trip.tripId(), "dup@test.de")).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteExternal(
            new InviteExternalCommand(TENANT_UUID, trip.tripId().value(), "dup@test.de",
                "Dup", "User", LocalDate.of(1990, 1, 1), ORGANIZER_ID)
        )).isInstanceOf(de.evia.travelmate.common.domain.DuplicateEntityException.class);
    }

    @Test
    void linkAwaitingInvitationsAutoAcceptsAndAddsParticipant() {
        final Trip trip = createTrip();
        final Invitation invitation = Invitation.inviteExternal(TENANT_ID, trip.tripId(), "new@test.de", ORGANIZER_ID);
        final UUID newMemberId = UUID.randomUUID();

        when(invitationRepository.findByInviteeEmailAndStatus("new@test.de", InvitationStatus.AWAITING_REGISTRATION))
            .thenReturn(List.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        invitationService.linkAwaitingInvitations("new@test.de", newMemberId, "New", "User");

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.inviteeId()).isEqualTo(newMemberId);
        assertThat(trip.hasParticipant(newMemberId)).isTrue();
        verify(invitationRepository).save(invitation);
        verify(tripRepository).save(trip);
    }

    @Test
    void inviteExternalCoalescesRegisteredPartyByEmail() {
        final Trip trip = createTrip();
        final TravelParty organizerParty = createPartyWithMembers(ORGANIZER_ID);
        final UUID targetMemberId = UUID.randomUUID();
        final UUID targetPartyTenantId = UUID.randomUUID();
        final TravelParty targetParty = TravelParty.create(new TenantId(targetPartyTenantId), "Familie Ziel");
        targetParty.addMember(targetMemberId, "known@test.de", "Known", "User");

        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        when(travelPartyRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(organizerParty));
        when(travelPartyRepository.findByMemberEmail("known@test.de")).thenReturn(Optional.of(targetParty));
        when(invitationRepository.existsByTripIdAndTargetPartyTenantIdInStatuses(
            trip.tripId(), targetPartyTenantId, List.of(InvitationStatus.PENDING, InvitationStatus.AWAITING_REGISTRATION)))
            .thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        final InvitationRepresentation result = invitationService.inviteExternal(
            new InviteExternalCommand(TENANT_UUID, trip.tripId().value(), "known@test.de",
                "Known", "User", LocalDate.of(1990, 1, 1), ORGANIZER_ID)
        );

        assertThat(result.inviteeId()).isEqualTo(targetMemberId);
        assertThat(result.targetPartyTenantId()).isEqualTo(targetPartyTenantId);
        verify(eventPublisher).publishEvent(any(InvitationCreated.class));
    }

    @Test
    void linkAwaitingInvitationsSkipsAlreadyExistingParticipant() {
        final Trip trip = createTrip();
        final UUID newMemberId = UUID.randomUUID();
        trip.addParticipant(newMemberId, "New", "User");
        final Invitation invitation = Invitation.inviteExternal(TENANT_ID, trip.tripId(), "new@test.de", ORGANIZER_ID);

        when(invitationRepository.findByInviteeEmailAndStatus("new@test.de", InvitationStatus.AWAITING_REGISTRATION))
            .thenReturn(List.of(invitation));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        invitationService.linkAwaitingInvitations("new@test.de", newMemberId, "New", "User");

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.inviteeId()).isEqualTo(newMemberId);
        verify(invitationRepository).save(invitation);
        verify(tripRepository, org.mockito.Mockito.never()).save(trip);
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
