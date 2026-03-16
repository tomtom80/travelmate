package de.evia.travelmate.expense.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TripParticipantTest {

    private static final UUID PARTICIPANT_ID = UUID.randomUUID();

    @Test
    void participantWithoutStayPeriodHasZeroNights() {
        final TripParticipant participant = new TripParticipant(PARTICIPANT_ID, "Alice");

        assertThat(participant.hasStayPeriod()).isFalse();
        assertThat(participant.nights()).isEqualTo(0);
    }

    @Test
    void participantWithStayPeriodCalculatesNights() {
        final TripParticipant participant = new TripParticipant(
            PARTICIPANT_ID, "Alice",
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)
        );

        assertThat(participant.hasStayPeriod()).isTrue();
        assertThat(participant.nights()).isEqualTo(7);
    }

    @Test
    void singleNightStay() {
        final TripParticipant participant = new TripParticipant(
            PARTICIPANT_ID, "Bob",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)
        );

        assertThat(participant.nights()).isEqualTo(1);
    }
}
