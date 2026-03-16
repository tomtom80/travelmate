package de.evia.travelmate.trips.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
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
        publishParticipantJoinedEvents(trip);
        return new TripRepresentation(trip);
    }

    private List<Participant> collectAllParticipants(final TravelParty party) {
        final List<Participant> participants = new ArrayList<>();
        party.members().forEach(m -> participants.add(
            new Participant(m.memberId(), m.firstName(), m.lastName())));
        party.dependents().forEach(d -> participants.add(
            new Participant(d.dependentId(), d.firstName(), d.lastName())));
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
        trip.setParticipantStayPeriod(
            command.participantId(),
            new StayPeriod(command.arrivalDate(), command.departureDate())
        );
        tripRepository.save(trip);
        publishEvents(trip);
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

    private void publishParticipantJoinedEvents(final Trip trip) {
        for (final Participant participant : trip.participants()) {
            final String name = participant.firstName() != null
                ? participant.firstName() + " " + participant.lastName()
                : participant.participantId().toString();
            eventPublisher.publishEvent(new ParticipantJoinedTrip(
                trip.tenantId().value(),
                trip.tripId().value(),
                participant.participantId(),
                name,
                LocalDate.now()
            ));
        }
    }
}
