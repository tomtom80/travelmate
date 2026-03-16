package de.evia.travelmate.expense.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;

class TripProjectionTest {

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void updateParticipantStayPeriodSetsArrivalAndDeparture() {
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Ski Trip");
        projection.addParticipant(new TripParticipant(ALICE, "Alice"));

        projection.updateParticipantStayPeriod(ALICE,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22));

        final TripParticipant updated = projection.participants().getFirst();
        assertThat(updated.arrivalDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(updated.departureDate()).isEqualTo(LocalDate.of(2026, 3, 22));
        assertThat(updated.nights()).isEqualTo(7);
    }

    @Test
    void updateParticipantStayPeriodIgnoresUnknownParticipant() {
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Ski Trip");
        projection.addParticipant(new TripParticipant(ALICE, "Alice"));

        projection.updateParticipantStayPeriod(BOB,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22));

        final TripParticipant unchanged = projection.participants().getFirst();
        assertThat(unchanged.hasStayPeriod()).isFalse();
    }

    @Test
    void updateParticipantStayPeriodPreservesOtherParticipants() {
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Ski Trip");
        projection.addParticipant(new TripParticipant(ALICE, "Alice"));
        projection.addParticipant(new TripParticipant(BOB, "Bob"));

        projection.updateParticipantStayPeriod(ALICE,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22));

        assertThat(projection.participants()).hasSize(2);
        final TripParticipant bob = projection.participants().stream()
            .filter(p -> p.participantId().equals(BOB)).findFirst().orElseThrow();
        assertThat(bob.hasStayPeriod()).isFalse();
    }
}
