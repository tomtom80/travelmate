package de.evia.travelmate.trips.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;

public class Trip extends AggregateRoot {

    private final TripId tripId;
    private final TenantId tenantId;
    private final TripName name;
    private final String description;
    private final DateRange dateRange;
    private final UUID organizerId;
    private final List<Participant> participants;
    private TripStatus status;

    public Trip(final TripId tripId,
                final TenantId tenantId,
                final TripName name,
                final String description,
                final DateRange dateRange,
                final UUID organizerId,
                final TripStatus status,
                final List<Participant> participants) {
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(name, "name");
        argumentIsNotNull(dateRange, "dateRange");
        argumentIsNotNull(organizerId, "organizerId");
        argumentIsNotNull(status, "status");
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.dateRange = dateRange;
        this.organizerId = organizerId;
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
            organizerId, TripStatus.PLANNING,
            participants
        );
        trip.registerEvent(new TripCreated(
            tenantId.value(),
            trip.tripId.value(),
            name.value(),
            dateRange.startDate(),
            dateRange.endDate(),
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

    public void setParticipantStayPeriod(final UUID participantId, final StayPeriod stayPeriod) {
        argumentIsNotNull(stayPeriod, "stayPeriod");
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

    public void confirm() {
        assertStatus(TripStatus.PLANNING, "confirm");
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

    public TripStatus status() {
        return status;
    }

    public List<Participant> participants() {
        return Collections.unmodifiableList(participants);
    }
}
