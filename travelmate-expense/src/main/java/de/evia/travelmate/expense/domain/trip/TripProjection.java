package de.evia.travelmate.expense.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public class TripProjection {

    private final UUID tripId;
    private final TenantId tenantId;
    private String tripName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal accommodationTotalPrice;
    private final List<TripParticipant> participants;

    public TripProjection(final UUID tripId, final TenantId tenantId,
                          final String tripName, final LocalDate startDate,
                          final LocalDate endDate, final List<TripParticipant> participants) {
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotBlank(tripName, "tripName");
        argumentIsNotNull(participants, "participants");
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.tripName = tripName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.participants = new ArrayList<>(participants);
    }

    public TripProjection(final UUID tripId, final TenantId tenantId,
                          final String tripName, final List<TripParticipant> participants) {
        this(tripId, tenantId, tripName, null, null, participants);
    }

    public static TripProjection create(final UUID tripId, final TenantId tenantId,
                                        final String tripName,
                                        final LocalDate startDate, final LocalDate endDate) {
        return new TripProjection(tripId, tenantId, tripName, startDate, endDate, List.of());
    }

    public static TripProjection create(final UUID tripId, final TenantId tenantId,
                                        final String tripName) {
        return new TripProjection(tripId, tenantId, tripName, null, null, List.of());
    }

    public void addParticipant(final TripParticipant participant) {
        argumentIsNotNull(participant, "participant");
        final boolean exists = participants.stream()
            .anyMatch(p -> p.participantId().equals(participant.participantId()));
        if (!exists) {
            participants.add(participant);
        }
    }

    public void updateParticipantStayPeriod(final UUID participantId,
                                             final LocalDate arrivalDate,
                                             final LocalDate departureDate) {
        argumentIsNotNull(participantId, "participantId");
        for (int i = 0; i < participants.size(); i++) {
            final TripParticipant p = participants.get(i);
            if (p.participantId().equals(participantId)) {
                participants.set(i, new TripParticipant(
                    p.participantId(), p.name(), arrivalDate, departureDate,
                    p.partyTenantId(), p.partyName(), p.dateOfBirth(), p.accountHolder()));
                return;
            }
        }
    }

    public void updatePartyName(final UUID partyTenantId, final String newName) {
        argumentIsNotNull(partyTenantId, "partyTenantId");
        argumentIsNotBlank(newName, "newName");
        for (int i = 0; i < participants.size(); i++) {
            final TripParticipant participant = participants.get(i);
            if (partyTenantId.equals(participant.partyTenantId())) {
                participants.set(i, participant.withPartyName(newName));
            }
        }
    }

    public void removeParticipant(final UUID participantId) {
        argumentIsNotNull(participantId, "participantId");
        participants.removeIf(participant -> participant.participantId().equals(participantId));
    }

    public void setAccommodationTotalPrice(final BigDecimal accommodationTotalPrice) {
        this.accommodationTotalPrice = accommodationTotalPrice;
    }

    public UUID tripId() { return tripId; }
    public TenantId tenantId() { return tenantId; }
    public String tripName() { return tripName; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public BigDecimal accommodationTotalPrice() { return accommodationTotalPrice; }
    public List<TripParticipant> participants() { return Collections.unmodifiableList(participants); }
}
