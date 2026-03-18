package de.evia.travelmate.expense.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record TripParticipant(UUID participantId, String name,
                               LocalDate arrivalDate, LocalDate departureDate,
                               UUID partyTenantId, String partyName) {

    public TripParticipant {
        argumentIsNotNull(participantId, "participantId");
        argumentIsNotBlank(name, "name");
    }

    public TripParticipant(final UUID participantId, final String name) {
        this(participantId, name, null, null, null, null);
    }

    public TripParticipant(final UUID participantId, final String name,
                            final LocalDate arrivalDate, final LocalDate departureDate) {
        this(participantId, name, arrivalDate, departureDate, null, null);
    }

    public boolean hasStayPeriod() {
        return arrivalDate != null && departureDate != null;
    }

    public boolean hasPartyInfo() {
        return partyTenantId != null && partyName != null;
    }

    public long nights() {
        if (!hasStayPeriod()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(arrivalDate, departureDate);
    }
}
