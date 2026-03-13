package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    private static final TenantId TENANT_A = new TenantId(UUID.randomUUID());
    private static final TenantId TENANT_B = new TenantId(UUID.randomUUID());

    @Autowired
    private TripRepository tripRepository;

    @Test
    void tripsFromTenantANotVisibleToTenantB() {
        final UUID organizerId = UUID.randomUUID();
        final Trip tripA = Trip.plan(
            TENANT_A,
            new TripName("Vacation A"),
            "Tenant A trip",
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14)),
            organizerId
        );
        tripRepository.save(tripA);

        final List<Trip> tenantATrips = tripRepository.findAllByTenantId(TENANT_A);
        final List<Trip> tenantBTrips = tripRepository.findAllByTenantId(TENANT_B);

        assertThat(tenantATrips).extracting(t -> t.tripId().value())
            .contains(tripA.tripId().value());
        assertThat(tenantBTrips).extracting(t -> t.tripId().value())
            .doesNotContain(tripA.tripId().value());
    }

    @Test
    void twoTenantsCanHaveTripsWithSameName() {
        final Trip tripA = Trip.plan(
            TENANT_A,
            new TripName("Beach Holiday"),
            null,
            new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14)),
            UUID.randomUUID()
        );
        final Trip tripB = Trip.plan(
            TENANT_B,
            new TripName("Beach Holiday"),
            null,
            new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14)),
            UUID.randomUUID()
        );
        tripRepository.save(tripA);
        tripRepository.save(tripB);

        final List<Trip> tenantATrips = tripRepository.findAllByTenantId(TENANT_A);
        final List<Trip> tenantBTrips = tripRepository.findAllByTenantId(TENANT_B);

        assertThat(tenantATrips).extracting(t -> t.tripId().value())
            .contains(tripA.tripId().value())
            .doesNotContain(tripB.tripId().value());
        assertThat(tenantBTrips).extracting(t -> t.tripId().value())
            .contains(tripB.tripId().value())
            .doesNotContain(tripA.tripId().value());
    }

    @Test
    void findByParticipantIdDoesNotLeakCrossTenant() {
        final UUID participantA = UUID.randomUUID();
        final UUID participantB = UUID.randomUUID();

        final Trip tripA = Trip.plan(
            TENANT_A,
            new TripName("Trip A"),
            null,
            new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7)),
            participantA
        );
        tripRepository.save(tripA);

        final Trip tripB = Trip.plan(
            TENANT_B,
            new TripName("Trip B"),
            null,
            new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7)),
            participantB
        );
        tripRepository.save(tripB);

        final List<Trip> participantATrips = tripRepository.findAllByParticipantId(participantA);
        final List<Trip> participantBTrips = tripRepository.findAllByParticipantId(participantB);

        assertThat(participantATrips).extracting(t -> t.tripId().value())
            .containsExactly(tripA.tripId().value());
        assertThat(participantBTrips).extracting(t -> t.tripId().value())
            .containsExactly(tripB.tripId().value());
    }
}
