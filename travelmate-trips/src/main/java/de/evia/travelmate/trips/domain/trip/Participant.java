package de.evia.travelmate.trips.domain.trip;

import java.util.UUID;

public class Participant {

    private final UUID participantId;
    private final String firstName;
    private final String lastName;
    private StayPeriod stayPeriod;

    public Participant(final UUID participantId) {
        this(participantId, null, null, null);
    }

    public Participant(final UUID participantId, final String firstName, final String lastName) {
        this(participantId, firstName, lastName, null);
    }

    public Participant(final UUID participantId, final StayPeriod stayPeriod) {
        this(participantId, null, null, stayPeriod);
    }

    public Participant(final UUID participantId, final String firstName, final String lastName,
                       final StayPeriod stayPeriod) {
        this.participantId = participantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.stayPeriod = stayPeriod;
    }

    public UUID participantId() {
        return participantId;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public StayPeriod stayPeriod() {
        return stayPeriod;
    }

    public void setStayPeriod(final StayPeriod stayPeriod) {
        this.stayPeriod = stayPeriod;
    }
}
