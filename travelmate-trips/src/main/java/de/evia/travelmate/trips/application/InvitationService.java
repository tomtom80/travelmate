package de.evia.travelmate.trips.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
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
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@Service
@Transactional
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final TripRepository tripRepository;
    private final TravelPartyRepository travelPartyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TripParticipationEventPublisher tripParticipationEventPublisher;

    public InvitationService(final InvitationRepository invitationRepository,
                             final TripRepository tripRepository,
                             final TravelPartyRepository travelPartyRepository,
                             final ApplicationEventPublisher eventPublisher,
                             final TripParticipationEventPublisher tripParticipationEventPublisher) {
        this.invitationRepository = invitationRepository;
        this.tripRepository = tripRepository;
        this.travelPartyRepository = travelPartyRepository;
        this.eventPublisher = eventPublisher;
        this.tripParticipationEventPublisher = tripParticipationEventPublisher;
    }

    public InvitationRepresentation invite(final InviteParticipantCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final TenantId tenantId = new TenantId(command.tenantId());

        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip", command.tripId().toString()));

        final TravelParty party = travelPartyRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", command.tenantId().toString()));

        if (!party.hasMember(command.inviteeId())) {
            throw new EntityNotFoundException("Member", command.inviteeId().toString());
        }

        final Member invitee = party.members().stream()
            .filter(m -> m.memberId().equals(command.inviteeId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Invitee member not found"));

        if (invitationRepository.existsByTripIdAndInviteeId(tripId, command.inviteeId())
            || invitationRepository.existsByTripIdAndTargetPartyTenantIdInStatuses(
                tripId,
                party.tenantId().value(),
                List.of(InvitationStatus.PENDING, InvitationStatus.AWAITING_REGISTRATION))) {
            throw new DuplicateEntityException("invitation.error.alreadyExists");
        }

        if (trip.hasParticipant(command.inviteeId())) {
            throw new DuplicateEntityException("participant.error.alreadyExists");
        }

        final Invitation invitation = Invitation.create(
            tenantId,
            tripId,
            command.inviteeId(),
            command.invitedBy(),
            party.tenantId().value()
        );
        invitationRepository.save(invitation);

        final Member inviter = party.members().stream()
            .filter(m -> m.memberId().equals(command.invitedBy()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Inviter member not found"));

        eventPublisher.publishEvent(new InvitationCreated(
            tenantId.value(),
            tripId.value(),
            invitation.invitationId().value(),
            invitee.email(),
            invitee.firstName(),
            trip.name().value(),
            trip.dateRange() != null ? trip.dateRange().startDate() : null,
            trip.dateRange() != null ? trip.dateRange().endDate() : null,
            inviter.firstName(),
            inviter.lastName(),
            LocalDate.now()
        ));

        return new InvitationRepresentation(invitation);
    }

    public void accept(final InvitationId invitationId, final UUID actorId) {
        final Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new EntityNotFoundException("Invitation", invitationId.value().toString()));
        assertInvitationActor(invitation, actorId);

        invitation.accept();

        final Trip trip = tripRepository.findById(invitation.tripId())
            .orElseThrow(() -> new EntityNotFoundException("Trip", invitation.tripId().value().toString()));

        final UUID targetPartyTenantId = invitation.targetPartyTenantId() != null
            ? invitation.targetPartyTenantId()
            : invitation.tenantId().value();
        final TravelParty party = travelPartyRepository.findByTenantId(new TenantId(targetPartyTenantId))
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", targetPartyTenantId.toString()));

        final Member member = party.members().stream()
            .filter(m -> m.memberId().equals(invitation.inviteeId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Member not found: " + invitation.inviteeId()));

        if (trip.hasParticipant(invitation.inviteeId())) {
            throw new DuplicateEntityException("participant.error.alreadyExists");
        }

        trip.addParticipant(invitation.inviteeId(), member.firstName(), member.lastName());
        tripRepository.save(trip);
        invitationRepository.save(invitation);

        tripParticipationEventPublisher.publishParticipantJoinedAfterCommit(new ParticipantJoinedTrip(
            invitation.tenantId().value(),
            trip.tripId().value(),
            invitation.inviteeId(),
            member.firstName() + " " + member.lastName(),
            party.tenantId().value(),
            party.name(),
            member.dateOfBirth(),
            true,
            LocalDate.now()
        ));
    }

    public void decline(final InvitationId invitationId, final UUID actorId) {
        final Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new EntityNotFoundException("Invitation", invitationId.value().toString()));
        assertInvitationActor(invitation, actorId);

        invitation.decline();
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationRepresentation> findByTripId(final TripId tripId) {
        return invitationRepository.findByTripId(tripId).stream()
            .map(InvitationRepresentation::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public InvitationRepresentation findById(final InvitationId invitationId) {
        return invitationRepository.findById(invitationId)
            .map(InvitationRepresentation::new)
            .orElseThrow(() -> new EntityNotFoundException("Invitation", invitationId.value().toString()));
    }

    public InvitationRepresentation inviteExternal(final InviteExternalCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final TenantId tenantId = new TenantId(command.tenantId());
        final String normalizedEmail = normalizeEmail(command.email());

        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip", command.tripId().toString()));

        final TravelParty party = travelPartyRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", command.tenantId().toString()));

        final Member inviter = party.members().stream()
            .filter(m -> m.memberId().equals(command.invitedBy()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Inviter member not found"));

        final TravelParty targetParty = travelPartyRepository.findByMemberEmail(normalizedEmail).orElse(null);
        if (targetParty != null) {
            if (invitationRepository.existsByTripIdAndTargetPartyTenantIdInStatuses(
                tripId,
                targetParty.tenantId().value(),
                List.of(InvitationStatus.PENDING, InvitationStatus.AWAITING_REGISTRATION))
                || partyAlreadyParticipates(trip, targetParty)) {
                throw new DuplicateEntityException("invitation.error.alreadyExists");
            }

            final Member invitee = targetParty.members().stream()
                .filter(member -> normalizeEmail(member.email()).equals(normalizedEmail))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Member", normalizedEmail));

            final Invitation invitation = Invitation.create(
                tenantId,
                tripId,
                invitee.memberId(),
                command.invitedBy(),
                targetParty.tenantId().value()
            );
            invitationRepository.save(invitation);

            eventPublisher.publishEvent(new InvitationCreated(
                tenantId.value(),
                tripId.value(),
                invitation.invitationId().value(),
                invitee.email(),
                invitee.firstName(),
                trip.name().value(),
                trip.dateRange() != null ? trip.dateRange().startDate() : null,
                trip.dateRange() != null ? trip.dateRange().endDate() : null,
                inviter.firstName(),
                inviter.lastName(),
                LocalDate.now()
            ));

            return new InvitationRepresentation(invitation);
        }

        if (invitationRepository.existsByTripIdAndInviteeEmail(tripId, normalizedEmail)) {
            throw new DuplicateEntityException("invitation.error.alreadyExists");
        }

        final Invitation invitation = Invitation.inviteExternal(tenantId, tripId, normalizedEmail, command.invitedBy());
        invitationRepository.save(invitation);

        eventPublisher.publishEvent(new InvitationCreated(
            tenantId.value(),
            tripId.value(),
            invitation.invitationId().value(),
            normalizedEmail,
            command.firstName(),
            trip.name().value(),
            trip.dateRange() != null ? trip.dateRange().startDate() : null,
            trip.dateRange() != null ? trip.dateRange().endDate() : null,
            inviter.firstName(),
            inviter.lastName(),
            LocalDate.now()
        ));

        eventPublisher.publishEvent(new ExternalUserInvitedToTrip(
            tenantId.value(),
            tripId.value(),
            invitation.invitationId().value(),
            normalizedEmail,
            command.firstName(),
            command.lastName(),
            command.dateOfBirth(),
            trip.name().value(),
            inviter.firstName(),
            inviter.lastName(),
            LocalDate.now()
        ));

        return new InvitationRepresentation(invitation);
    }

    public void linkAwaitingInvitations(final String email, final UUID memberId,
                                        final String firstName, final String lastName) {
        final List<Invitation> awaiting = invitationRepository.findByInviteeEmailAndStatus(
            email, InvitationStatus.AWAITING_REGISTRATION);

        final TravelParty memberParty = travelPartyRepository.findByMemberEmail(email).orElse(null);

        for (final Invitation invitation : awaiting) {
            invitation.linkToMember(memberId, memberParty != null ? memberParty.tenantId().value() : null);

            final Trip trip = tripRepository.findById(invitation.tripId())
                .orElseThrow(() -> new IllegalStateException("Trip not found: " + invitation.tripId().value()));

            if (trip.hasParticipant(memberId)) {
                invitationRepository.save(invitation);
                continue;
            }

            trip.addParticipant(memberId, firstName, lastName);
            tripRepository.save(trip);
            invitationRepository.save(invitation);

            final UUID partyTenantId = memberParty != null ? memberParty.tenantId().value() : null;
            final String partyName = memberParty != null ? memberParty.name() : null;
            tripParticipationEventPublisher.publishParticipantJoinedAfterCommit(new ParticipantJoinedTrip(
                invitation.tenantId().value(),
                trip.tripId().value(),
                memberId,
                firstName + " " + lastName,
                partyTenantId,
                partyName,
                memberParty != null
                    ? memberParty.findMember(memberId).map(Member::dateOfBirth).orElse(null)
                    : null,
                true,
                LocalDate.now()
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<InvitationRepresentation> findPendingByInviteeId(final UUID inviteeId) {
        return invitationRepository.findByInviteeIdAndStatus(inviteeId, InvitationStatus.PENDING).stream()
            .map(InvitationRepresentation::new)
            .toList();
    }

    private boolean partyAlreadyParticipates(final Trip trip, final TravelParty targetParty) {
        return trip.participants().stream()
            .anyMatch(participant -> targetParty.hasParticipant(participant.participantId()));
    }

    private String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void assertInvitationActor(final Invitation invitation, final UUID actorId) {
        if (!actorId.equals(invitation.inviteeId())) {
            throw new IllegalArgumentException("Only the invited member can act on this invitation.");
        }
    }
}
