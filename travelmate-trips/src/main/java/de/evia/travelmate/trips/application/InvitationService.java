package de.evia.travelmate.trips.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.domain.invitation.Invitation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.invitation.InvitationRepository;
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

    public InvitationService(final InvitationRepository invitationRepository,
                             final TripRepository tripRepository,
                             final TravelPartyRepository travelPartyRepository,
                             final ApplicationEventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.tripRepository = tripRepository;
        this.travelPartyRepository = travelPartyRepository;
        this.eventPublisher = eventPublisher;
    }

    public InvitationRepresentation invite(final InviteParticipantCommand command) {
        final TripId tripId = new TripId(command.tripId());
        final TenantId tenantId = new TenantId(command.tenantId());

        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + command.tripId()));

        final TravelParty party = travelPartyRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalStateException("TravelParty not found for tenant " + command.tenantId()));

        if (!party.hasMember(command.inviteeId())) {
            throw new IllegalArgumentException(
                "Invitee " + command.inviteeId() + " is not a member of the travel party.");
        }

        if (invitationRepository.existsByTripIdAndInviteeId(tripId, command.inviteeId())) {
            throw new IllegalArgumentException(
                "Invitation already exists for trip " + command.tripId() + " and invitee " + command.inviteeId());
        }

        final Invitation invitation = Invitation.create(tenantId, tripId, command.inviteeId(), command.invitedBy());
        invitationRepository.save(invitation);
        return new InvitationRepresentation(invitation);
    }

    public void accept(final InvitationId invitationId) {
        final Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId.value()));

        invitation.accept();

        final Trip trip = tripRepository.findById(invitation.tripId())
            .orElseThrow(() -> new IllegalStateException("Trip not found: " + invitation.tripId().value()));

        final TravelParty party = travelPartyRepository.findByTenantId(invitation.tenantId())
            .orElseThrow(() -> new IllegalStateException("TravelParty not found"));

        final Member member = party.members().stream()
            .filter(m -> m.memberId().equals(invitation.inviteeId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Member not found: " + invitation.inviteeId()));

        trip.addParticipant(invitation.inviteeId());
        tripRepository.save(trip);
        invitationRepository.save(invitation);

        eventPublisher.publishEvent(new ParticipantJoinedTrip(
            invitation.tenantId().value(),
            trip.tripId().value(),
            invitation.inviteeId(),
            member.email(),
            LocalDate.now()
        ));
    }

    public void decline(final InvitationId invitationId) {
        final Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId.value()));

        invitation.decline();
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationRepresentation> findByTripId(final TripId tripId) {
        return invitationRepository.findByTripId(tripId).stream()
            .map(InvitationRepresentation::new)
            .toList();
    }
}
