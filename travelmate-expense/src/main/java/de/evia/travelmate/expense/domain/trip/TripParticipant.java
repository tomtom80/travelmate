package de.evia.travelmate.expense.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record TripParticipant(UUID participantId, String name,
                               LocalDate arrivalDate, LocalDate departureDate,
                               UUID partyTenantId, String partyName,
                               LocalDate dateOfBirth, boolean accountHolder) {

    public TripParticipant {
        argumentIsNotNull(participantId, "participantId");
        argumentIsNotBlank(name, "name");
    }

    public TripParticipant(final UUID participantId, final String name) {
        this(participantId, name, null, null, null, null, null, false);
    }

    public TripParticipant(final UUID participantId, final String name,
                            final LocalDate arrivalDate, final LocalDate departureDate) {
        this(participantId, name, arrivalDate, departureDate, null, null, null, false);
    }

    public TripParticipant(final UUID participantId, final String name,
                           final LocalDate arrivalDate, final LocalDate departureDate,
                           final UUID partyTenantId, final String partyName) {
        this(participantId, name, arrivalDate, departureDate, partyTenantId, partyName, null, false);
    }

    public boolean hasStayPeriod() {
        return arrivalDate != null && departureDate != null;
    }

    public boolean hasPartyInfo() {
        return partyTenantId != null && partyName != null;
    }

    public TripParticipant withPartyName(final String newPartyName) {
        return new TripParticipant(
            participantId,
            name,
            arrivalDate,
            departureDate,
            partyTenantId,
            newPartyName,
            dateOfBirth,
            accountHolder
        );
    }

    public long nights() {
        if (!hasStayPeriod()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(arrivalDate, departureDate);
    }

    public Integer ageOn(final LocalDate date) {
        if (dateOfBirth == null || date == null || date.isBefore(dateOfBirth)) {
            return null;
        }
        return Period.between(dateOfBirth, date).getYears();
    }
}
