package de.evia.travelmate.trips.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.AddParticipantToTripCommand;
import de.evia.travelmate.trips.application.command.GrantTripOrganizerCommand;
import de.evia.travelmate.trips.application.command.RemoveParticipantFromTripCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Participant;
import de.evia.travelmate.trips.domain.trip.StayPeriod;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@Service
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final TravelPartyRepository travelPartyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TripService(final TripRepository tripRepository,
                       final TravelPartyRepository travelPartyRepository,
                       final ApplicationEventPublisher eventPublisher) {
        this.tripRepository = tripRepository;
        this.travelPartyRepository = travelPartyRepository;
        this.eventPublisher = eventPublisher;
    }

    public TripRepresentation createTrip(final CreateTripCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TravelParty party = travelPartyRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalStateException(
                "TravelParty not found for tenant " + command.tenantId()));

        if (!party.hasMember(command.organizerId())) {
            throw new IllegalArgumentException(
                "Organizer " + command.organizerId() + " is not a member of the travel party.");
        }

        final List<Participant> participants = collectAllParticipants(party);

        final Trip trip = Trip.planWithParticipants(
            tenantId,
            new TripName(command.name()),
            command.description(),
            new DateRange(command.startDate(), command.endDate()),
            command.organizerId(),
            participants
        );

        tripRepository.save(trip);
        publishEvents(trip);
        publishParticipantJoinedEvents(trip, party);
        return new TripRepresentation(trip);
    }

    private List<Participant> collectAllParticipants(final TravelParty party) {
        final List<Participant> participants = new ArrayList<>();
        final java.util.Set<UUID> seen = new java.util.HashSet<>();
        party.members().forEach(m -> {
            if (seen.add(m.memberId())) {
                participants.add(new Participant(m.memberId(), m.firstName(), m.lastName()));
            }
        });
        party.dependents().forEach(d -> {
            if (seen.add(d.dependentId())) {
                participants.add(new Participant(d.dependentId(), d.firstName(), d.lastName()));
            }
        });
        return participants;
    }

    @Transactional(readOnly = true)
    public TripRepresentation findById(final TripId tripId) {
        return new TripRepresentation(findTrip(tripId));
    }

    @Transactional(readOnly = true)
    public List<TripRepresentation> findAllByTenantId(final TenantId tenantId) {
        return tripRepository.findAllByTenantId(tenantId).stream()
            .map(TripRepresentation::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TripRepresentation> findAllByParticipantId(final UUID participantId) {
        return tripRepository.findAllByParticipantId(participantId).stream()
            .map(TripRepresentation::new)
            .toList();
    }

    public void confirmTrip(final TripId tripId) {
        final Trip trip = findTrip(tripId);
        trip.confirm();
        tripRepository.save(trip);
    }

    public void startTrip(final TripId tripId) {
        final Trip trip = findTrip(tripId);
        trip.start();
        tripRepository.save(trip);
    }

    public void completeTrip(final TripId tripId) {
        final Trip trip = findTrip(tripId);
        trip.complete();
        tripRepository.save(trip);
        publishEvents(trip);
    }

    public void cancelTrip(final TripId tripId) {
        final Trip trip = findTrip(tripId);
        trip.cancel();
        tripRepository.save(trip);
    }

    public void setStayPeriod(final SetStayPeriodCommand command) {
        final Trip trip = findTrip(new TripId(command.tripId()));
        final TravelParty actorParty = travelPartyRepository.findByTenantId(new TenantId(command.actorPartyTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", command.actorPartyTenantId().toString()));
        final boolean isTripOrganizer = trip.isOrganizer(command.actorId());
        final boolean actsWithinOwnParty = actorParty.hasParticipant(command.actorId())
            && actorParty.hasParticipant(command.participantId());
        if (!isTripOrganizer && !actsWithinOwnParty) {
            throw new IllegalArgumentException(
                "Only the trip organizer or members of the participant's own travel party can update the stay period.");
        }
        trip.setParticipantStayPeriod(
            command.participantId(),
            new StayPeriod(command.arrivalDate(), command.departureDate())
        );
        tripRepository.save(trip);
        publishEvents(trip);
    }

    public void addParticipantToTrip(final AddParticipantToTripCommand command) {
        final Trip trip = findTrip(new TripId(command.tripId()));
        final TravelParty actorParty = travelPartyRepository.findByTenantId(new TenantId(command.actorPartyTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", command.actorPartyTenantId().toString()));
        assertActorCanManageOwnPartyParticipant(actorParty, command.actorId(), command.participantId());

        if (trip.hasParticipant(command.participantId())) {
            throw new DuplicateEntityException("participant.error.alreadyExists");
        }

        final var member = actorParty.members().stream()
            .filter(m -> m.memberId().equals(command.participantId()))
            .findFirst()
            .orElse(null);
        if (member != null) {
            trip.addParticipant(member.memberId(), member.firstName(), member.lastName());
            tripRepository.save(trip);
            eventPublisher.publishEvent(new ParticipantJoinedTrip(
                trip.tenantId().value(),
                trip.tripId().value(),
                member.memberId(),
                member.firstName() + " " + member.lastName(),
                actorParty.tenantId().value(),
                actorParty.name(),
                member.dateOfBirth(),
                true,
                LocalDate.now()
            ));
            return;
        }

        final var dependent = actorParty.dependents().stream()
            .filter(d -> d.dependentId().equals(command.participantId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Only participants from the actor's own travel party can be added."));
        trip.addParticipant(dependent.dependentId(), dependent.firstName(), dependent.lastName());
        tripRepository.save(trip);
        eventPublisher.publishEvent(new ParticipantJoinedTrip(
            trip.tenantId().value(),
            trip.tripId().value(),
            dependent.dependentId(),
            dependent.firstName() + " " + dependent.lastName(),
            actorParty.tenantId().value(),
            actorParty.name(),
            dependent.dateOfBirth(),
            false,
            LocalDate.now()
        ));
    }

    public void removeParticipantFromTrip(final RemoveParticipantFromTripCommand command) {
        final Trip trip = findTrip(new TripId(command.tripId()));
        final TravelParty actorParty = travelPartyRepository.findByTenantId(new TenantId(command.actorPartyTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("TravelParty", command.actorPartyTenantId().toString()));
        assertActorCanManageOwnPartyParticipant(actorParty, command.actorId(), command.participantId());

        trip.removeParticipant(command.participantId());
        tripRepository.save(trip);
    }

    public void grantTripOrganizer(final GrantTripOrganizerCommand command) {
        final Trip trip = findTrip(new TripId(command.tripId()));
        if (!trip.isOrganizer(command.actorId())) {
            throw new IllegalArgumentException("Only an existing trip organizer can grant organizer rights.");
        }
        final boolean participantHasAccount = travelPartyRepository.findAll().stream()
            .flatMap(party -> party.members().stream())
            .anyMatch(member -> member.memberId().equals(command.participantId()));
        if (!participantHasAccount) {
            throw new IllegalArgumentException(
                "Only participants with an account can be granted organizer rights."
            );
        }
        trip.grantOrganizerRights(command.participantId());
        tripRepository.save(trip);
    }

    private Trip findTrip(final TripId tripId) {
        return tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip", tripId.value().toString()));
    }

    private void publishEvents(final Trip trip) {
        for (final DomainEvent event : trip.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        trip.clearDomainEvents();
    }

    private void publishParticipantJoinedEvents(final Trip trip, final TravelParty party) {
        for (final Participant participant : trip.participants()) {
            final var member = party.findMember(participant.participantId());
            if (member.isPresent()) {
                eventPublisher.publishEvent(new ParticipantJoinedTrip(
                    trip.tenantId().value(),
                    trip.tripId().value(),
                    participant.participantId(),
                    member.get().firstName() + " " + member.get().lastName(),
                    party.tenantId().value(),
                    party.name(),
                    member.get().dateOfBirth(),
                    true,
                    LocalDate.now()
                ));
                continue;
            }
            final var dependent = party.findDependent(participant.participantId())
                .orElseThrow(() -> new IllegalStateException(
                    "Participant " + participant.participantId() + " missing from travel party projection."));
            eventPublisher.publishEvent(new ParticipantJoinedTrip(
                trip.tenantId().value(),
                trip.tripId().value(),
                participant.participantId(),
                dependent.firstName() + " " + dependent.lastName(),
                party.tenantId().value(),
                party.name(),
                dependent.dateOfBirth(),
                false,
                LocalDate.now()
            ));
        }
    }

    private void assertActorCanManageOwnPartyParticipant(final TravelParty actorParty,
                                                         final UUID actorId,
                                                         final UUID participantId) {
        if (!actorParty.hasMember(actorId)) {
            throw new IllegalArgumentException("Actor is not a member of the given travel party.");
        }
        if (!actorParty.hasParticipant(participantId)) {
            throw new IllegalArgumentException("Only participants from the actor's own travel party can be managed.");
        }
    }
}
