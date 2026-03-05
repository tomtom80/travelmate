package de.evia.travelmate.trips.application;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
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

        final Trip trip = Trip.plan(
            tenantId,
            new TripName(command.name()),
            command.description(),
            new DateRange(command.startDate(), command.endDate()),
            command.organizerId()
        );

        tripRepository.save(trip);
        publishEvents(trip);
        return new TripRepresentation(trip);
    }

    @Transactional(readOnly = true)
    public TripRepresentation findById(final TripId tripId) {
        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId.value()));
        return new TripRepresentation(trip);
    }

    @Transactional(readOnly = true)
    public List<TripRepresentation> findAllByTenantId(final TenantId tenantId) {
        return tripRepository.findAllByTenantId(tenantId).stream()
            .map(TripRepresentation::new)
            .toList();
    }

    private void publishEvents(final Trip trip) {
        for (final DomainEvent event : trip.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        trip.clearDomainEvents();
    }
}
