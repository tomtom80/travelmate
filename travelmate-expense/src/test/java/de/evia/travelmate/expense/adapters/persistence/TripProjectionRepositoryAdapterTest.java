package de.evia.travelmate.expense.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@SpringBootTest
@ActiveProfiles("test")
class TripProjectionRepositoryAdapterTest {

    @Autowired
    private TripProjectionRepository tripProjectionRepository;

    @Test
    void savesAndFindsByTripId() {
        final UUID tripId = UUID.randomUUID();
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripProjection projection = TripProjection.create(tripId, tenantId, "Summer Vacation");

        tripProjectionRepository.save(projection);

        final Optional<TripProjection> found = tripProjectionRepository.findByTripId(tripId);
        assertThat(found).isPresent();
        assertThat(found.get().tripId()).isEqualTo(tripId);
        assertThat(found.get().tenantId()).isEqualTo(tenantId);
        assertThat(found.get().tripName()).isEqualTo("Summer Vacation");
    }

    @Test
    void persistsParticipants() {
        final UUID tripId = UUID.randomUUID();
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID participantId1 = UUID.randomUUID();
        final UUID participantId2 = UUID.randomUUID();
        final TripProjection projection = TripProjection.create(tripId, tenantId, "Ski Trip");
        projection.addParticipant(new TripParticipant(
            participantId1, "Alice", null, null, UUID.randomUUID(), "Familie A", LocalDate.of(1987, 3, 4), true));
        projection.addParticipant(new TripParticipant(
            participantId2, "Bob", null, null, UUID.randomUUID(), "Familie B", LocalDate.of(2021, 6, 8), false));

        tripProjectionRepository.save(projection);

        final Optional<TripProjection> found = tripProjectionRepository.findByTripId(tripId);
        assertThat(found).isPresent();
        assertThat(found.get().participants()).hasSize(2);
        assertThat(found.get().participants())
            .extracting(TripParticipant::name)
            .containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(found.get().participants())
            .extracting(TripParticipant::dateOfBirth)
            .containsExactlyInAnyOrder(LocalDate.of(1987, 3, 4), LocalDate.of(2021, 6, 8));
        assertThat(found.get().participants())
            .extracting(TripParticipant::accountHolder)
            .containsExactlyInAnyOrder(true, false);
    }

    @Test
    void existsByTripIdReturnsTrue() {
        final UUID tripId = UUID.randomUUID();
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripProjection projection = TripProjection.create(tripId, tenantId, "Beach Holiday");

        tripProjectionRepository.save(projection);

        assertThat(tripProjectionRepository.existsByTripId(tripId)).isTrue();
    }

    @Test
    void existsByTripIdReturnsFalse() {
        assertThat(tripProjectionRepository.existsByTripId(UUID.randomUUID())).isFalse();
    }

    @Test
    void persistsStayPeriodDates() {
        final UUID tripId = UUID.randomUUID();
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID participantId = UUID.randomUUID();
        final TripProjection projection = TripProjection.create(tripId, tenantId, "Ski Trip");
        projection.addParticipant(new TripParticipant(participantId, "Alice"));
        tripProjectionRepository.save(projection);

        final TripProjection loaded = tripProjectionRepository.findByTripId(tripId).orElseThrow();
        loaded.updateParticipantStayPeriod(participantId,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22));
        tripProjectionRepository.save(loaded);

        final TripProjection reloaded = tripProjectionRepository.findByTripId(tripId).orElseThrow();
        final TripParticipant participant = reloaded.participants().getFirst();
        assertThat(participant.arrivalDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(participant.departureDate()).isEqualTo(LocalDate.of(2026, 3, 22));
        assertThat(participant.hasStayPeriod()).isTrue();
        assertThat(participant.nights()).isEqualTo(7);
    }

    @Test
    void updatesExistingProjection() {
        final UUID tripId = UUID.randomUUID();
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID participantId = UUID.randomUUID();
        final TripProjection projection = TripProjection.create(tripId, tenantId, "Road Trip");

        tripProjectionRepository.save(projection);

        final TripProjection updated = new TripProjection(
            tripId, tenantId, "Updated Road Trip",
            java.util.List.of(new TripParticipant(
                participantId, "Charlie", null, null, null, null, LocalDate.of(2018, 5, 1), false))
        );
        tripProjectionRepository.save(updated);

        final Optional<TripProjection> found = tripProjectionRepository.findByTripId(tripId);
        assertThat(found).isPresent();
        assertThat(found.get().tripName()).isEqualTo("Updated Road Trip");
        assertThat(found.get().participants()).hasSize(1);
        assertThat(found.get().participants().getFirst().name()).isEqualTo("Charlie");
        assertThat(found.get().participants().getFirst().dateOfBirth()).isEqualTo(LocalDate.of(2018, 5, 1));
    }
}
