package de.evia.travelmate.expense.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public class TripProjection {

    private final UUID tripId;
    private final TenantId tenantId;
    private String tripName;
    private final List<TripParticipant> participants;

    public TripProjection(final UUID tripId, final TenantId tenantId,
                          final String tripName, final List<TripParticipant> participants) {
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotBlank(tripName, "tripName");
        argumentIsNotNull(participants, "participants");
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.tripName = tripName;
        this.participants = new ArrayList<>(participants);
    }

    public static TripProjection create(final UUID tripId, final TenantId tenantId,
                                        final String tripName) {
        return new TripProjection(tripId, tenantId, tripName, List.of());
    }

    public void addParticipant(final TripParticipant participant) {
        argumentIsNotNull(participant, "participant");
        final boolean exists = participants.stream()
            .anyMatch(p -> p.participantId().equals(participant.participantId()));
        if (!exists) {
            participants.add(participant);
        }
    }

    public UUID tripId() { return tripId; }
    public TenantId tenantId() { return tenantId; }
    public String tripName() { return tripName; }
    public List<TripParticipant> participants() { return Collections.unmodifiableList(participants); }
}
