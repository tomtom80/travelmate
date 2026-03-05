package de.evia.travelmate.trips.domain.trip;

import java.util.UUID;

public class Participant {

    private final UUID participantId;
    private StayPeriod stayPeriod;

    public Participant(final UUID participantId) {
        this.participantId = participantId;
    }

    public Participant(final UUID participantId, final StayPeriod stayPeriod) {
        this.participantId = participantId;
        this.stayPeriod = stayPeriod;
    }

    public UUID participantId() {
        return participantId;
    }

    public StayPeriod stayPeriod() {
        return stayPeriod;
    }

    public void setStayPeriod(final StayPeriod stayPeriod) {
        this.stayPeriod = stayPeriod;
    }
}
