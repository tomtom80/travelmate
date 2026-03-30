package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantRemovedFromTrip;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;

public class Trip extends AggregateRoot {

    private final TripId tripId;
    private final TenantId tenantId;
    private final TripName name;
    private final String description;
    private DateRange dateRange;
    private final UUID organizerId;
    private final List<UUID> organizerIds;
    private final List<Participant> participants;
    private TripStatus status;

    public Trip(final TripId tripId,
                final TenantId tenantId,
                final TripName name,
                final String description,
                final DateRange dateRange,
                final UUID organizerId,
                final List<UUID> organizerIds,
                final TripStatus status,
                final List<Participant> participants) {
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(name, "name");
        argumentIsNotNull(organizerId, "organizerId");
        argumentIsNotNull(organizerIds, "organizerIds");
        argumentIsNotNull(status, "status");
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.dateRange = dateRange;
        this.organizerId = organizerId;
        this.organizerIds = new ArrayList<>(organizerIds);
        this.status = status;
        this.participants = new ArrayList<>(participants);
    }

    public static Trip plan(final TenantId tenantId,
                            final TripName name,
                            final String description,
                            final DateRange dateRange,
                            final UUID organizerId) {
        return plan(tenantId, name, description, dateRange, organizerId, List.of(organizerId));
    }

    public static Trip plan(final TenantId tenantId,
                            final TripName name,
                            final String description,
                            final DateRange dateRange,
                            final UUID organizerId,
                            final List<UUID> participantIds) {
        final List<Participant> participants = participantIds.stream()
            .map(id -> new Participant(id))
            .toList();
        return planWithParticipants(tenantId, name, description, dateRange, organizerId, participants);
    }

    public static Trip planWithParticipants(final TenantId tenantId,
                                            final TripName name,
                                            final String description,
                                            final DateRange dateRange,
                                            final UUID organizerId,
                                            final List<Participant> participants) {
        final Trip trip = new Trip(
            new TripId(UUID.randomUUID()),
            tenantId, name, description, dateRange,
            organizerId, List.of(organizerId), TripStatus.PLANNING,
            participants
        );
        trip.registerEvent(new TripCreated(
            tenantId.value(),
            trip.tripId.value(),
            name.value(),
            dateRange != null ? dateRange.startDate() : null,
            dateRange != null ? dateRange.endDate() : null,
            LocalDate.now()
        ));
        return trip;
    }

    public void addParticipant(final UUID participantId) {
        addParticipant(participantId, null, null);
    }

    public void addParticipant(final UUID participantId, final String firstName, final String lastName) {
        if (hasParticipant(participantId)) {
            throw new IllegalArgumentException(
                "Participant " + participantId + " already joined this trip.");
        }
        participants.add(new Participant(participantId, firstName, lastName));
    }

    public void removeParticipant(final UUID participantId) {
        if (isOrganizer(participantId)) {
            throw new IllegalArgumentException("Trip organizer cannot be removed from the trip.");
        }
        final boolean removed = participants.removeIf(p -> p.participantId().equals(participantId));
        if (!removed) {
            throw new IllegalArgumentException("Participant " + participantId + " not found in this trip.");
        }
        registerEvent(new ParticipantRemovedFromTrip(
            tenantId.value(),
            tripId.value(),
            participantId,
            LocalDate.now()
        ));
    }

    public void grantOrganizerRights(final UUID participantId) {
        if (!hasParticipant(participantId)) {
            throw new IllegalArgumentException("Participant " + participantId + " not found in this trip.");
        }
        if (!organizerIds.contains(participantId)) {
            organizerIds.add(participantId);
        }
    }

    public void setParticipantStayPeriod(final UUID participantId, final StayPeriod stayPeriod) {
        argumentIsNotNull(stayPeriod, "stayPeriod");
        if (dateRange == null) {
            throw new IllegalStateException("Stay periods can only be set after the trip date range was confirmed.");
        }
        if (!stayPeriod.isWithin(dateRange)) {
            throw new IllegalArgumentException(
                "Stay period must be within the trip date range.");
        }
        final Participant participant = participants.stream()
            .filter(p -> p.participantId().equals(participantId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Participant " + participantId + " not found in this trip."));
        participant.setStayPeriod(stayPeriod);
        registerEvent(new StayPeriodUpdated(
            tenantId.value(), tripId.value(), participantId,
            stayPeriod.arrivalDate(), stayPeriod.departureDate(), LocalDate.now()
        ));
    }

    public boolean hasParticipant(final UUID participantId) {
        return participants.stream()
            .anyMatch(p -> p.participantId().equals(participantId));
    }

    public boolean isOrganizer(final UUID participantId) {
        return organizerIds.contains(participantId);
    }

    public void confirm() {
        assertStatus(TripStatus.PLANNING, "confirm");
        if (dateRange == null) {
            throw new IllegalStateException("Cannot confirm a trip without a final date range.");
        }
        this.status = TripStatus.CONFIRMED;
    }

    public void start() {
        assertStatus(TripStatus.CONFIRMED, "start");
        this.status = TripStatus.IN_PROGRESS;
    }

    public void complete() {
        assertStatus(TripStatus.IN_PROGRESS, "complete");
        this.status = TripStatus.COMPLETED;
        registerEvent(new TripCompleted(tenantId.value(), tripId.value(), LocalDate.now()));
    }

    public void cancel() {
        if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) {
            throw new IllegalStateException(
                "Cannot cancel trip in status " + status);
        }
        this.status = TripStatus.CANCELLED;
    }

    public void updateDateRange(final DateRange newDateRange) {
        argumentIsNotNull(newDateRange, "newDateRange");
        assertStatus(TripStatus.PLANNING, "update date range");
        this.dateRange = newDateRange;
        for (final Participant participant : participants) {
            if (participant.stayPeriod() != null && !participant.stayPeriod().isWithin(newDateRange)) {
                participant.setStayPeriod(null);
            }
        }
    }

    private void assertStatus(final TripStatus expected, final String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                "Cannot " + action + " trip in status " + this.status + " (expected: " + expected + ")");
        }
    }

    public TripId tripId() {
        return tripId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripName name() {
        return name;
    }

    public String description() {
        return description;
    }

    public DateRange dateRange() {
        return dateRange;
    }

    public UUID organizerId() {
        return organizerId;
    }

    public List<UUID> organizerIds() {
        return Collections.unmodifiableList(organizerIds);
    }

    public TripStatus status() {
        return status;
    }

    public List<Participant> participants() {
        return Collections.unmodifiableList(participants);
    }
}
