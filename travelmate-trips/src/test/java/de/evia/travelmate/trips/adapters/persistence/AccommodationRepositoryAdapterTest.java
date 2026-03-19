package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.accommodation.Accommodation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationRepository;
import de.evia.travelmate.trips.domain.accommodation.Room;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class AccommodationRepositoryAdapterTest {

    @Autowired
    private AccommodationRepository repository;

    @Test
    void savesAndFindsAccommodationByTripId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Berghuette",
            "Alpweg 12", "https://booking.com/123",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            new BigDecimal("3000.00"),
            List.of(
                new Room("Zimmer 1", 2, new BigDecimal("80.00")),
                new Room("Matratzenlager", 8, null)
            )
        );

        repository.save(accommodation);

        final Optional<Accommodation> found = repository.findByTripId(tripId);
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Berghuette");
        assertThat(found.get().address()).isEqualTo("Alpweg 12");
        assertThat(found.get().url()).isEqualTo("https://booking.com/123");
        assertThat(found.get().checkIn()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(found.get().checkOut()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(found.get().totalPrice()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(found.get().rooms()).hasSize(2);
    }

    @Test
    void existsByTripIdReturnsTrueWhenExists() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", 1, null))
        );
        repository.save(accommodation);

        assertThat(repository.existsByTripId(tripId)).isTrue();
    }

    @Test
    void existsByTripIdReturnsFalseWhenNotExists() {
        assertThat(repository.existsByTripId(new TripId(UUID.randomUUID()))).isFalse();
    }

    @Test
    void updatesAccommodationDetails() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Old Name", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", 2, null))
        );
        repository.save(accommodation);

        final Accommodation loaded = repository.findByTripId(tripId).orElseThrow();
        loaded.updateDetails("New Name", "New Address", "https://new.com",
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), new BigDecimal("500.00"));
        repository.save(loaded);

        final Accommodation reloaded = repository.findByTripId(tripId).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("New Name");
        assertThat(reloaded.address()).isEqualTo("New Address");
        assertThat(reloaded.totalPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void deleteByTripIdRemovesAccommodation() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", 1, null))
        );
        repository.save(accommodation);

        repository.deleteByTripId(tripId);

        assertThat(repository.findByTripId(tripId)).isEmpty();
    }

    @Test
    void savesAndLoadsRoomDetails() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("4-Bett-Zimmer", 4, new BigDecimal("120.00")))
        );
        repository.save(accommodation);

        final Accommodation loaded = repository.findByTripId(tripId).orElseThrow();
        final Room room = loaded.rooms().getFirst();
        assertThat(room.name()).isEqualTo("4-Bett-Zimmer");
        assertThat(room.bedCount()).isEqualTo(4);
        assertThat(room.pricePerNight()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    void savesAndLoadsRoomAssignment() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", 2, null))
        );
        final UUID partyId = UUID.randomUUID();
        accommodation.assignPartyToRoom(
            accommodation.rooms().getFirst().roomId(), partyId, "Familie Schmidt", 2
        );
        repository.save(accommodation);

        final Accommodation loaded = repository.findByTripId(tripId).orElseThrow();
        assertThat(loaded.assignments()).hasSize(1);
        assertThat(loaded.assignments().getFirst().partyTenantId()).isEqualTo(partyId);
        assertThat(loaded.assignments().getFirst().partyName()).isEqualTo("Familie Schmidt");
        assertThat(loaded.assignments().getFirst().personCount()).isEqualTo(2);
        assertThat(loaded.assignments().getFirst().assignedAt()).isNotNull();
    }

    @Test
    void savesMultipleAssignmentsPerRoom() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("Matratzenlager", 10, null))
        );
        final var roomId = accommodation.rooms().getFirst().roomId();
        accommodation.assignPartyToRoom(roomId, UUID.randomUUID(), "Familie A", 3);
        accommodation.assignPartyToRoom(roomId, UUID.randomUUID(), "Familie B", 4);
        repository.save(accommodation);

        final Accommodation loaded = repository.findByTripId(tripId).orElseThrow();
        assertThat(loaded.assignments()).hasSize(2);
        assertThat(loaded.totalAssignedPersons()).isEqualTo(7);
    }

    @Test
    void removesAssignmentOnSave() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final Accommodation accommodation = Accommodation.create(
            new TenantId(UUID.randomUUID()), tripId, "Huette", null, null,
            null, null, null,
            List.of(new Room("Zimmer 1", 2, null))
        );
        accommodation.assignPartyToRoom(
            accommodation.rooms().getFirst().roomId(), UUID.randomUUID(), "Familie Schmidt", 2
        );
        repository.save(accommodation);

        final Accommodation loaded = repository.findByTripId(tripId).orElseThrow();
        loaded.removeAssignment(loaded.assignments().getFirst().assignmentId());
        repository.save(loaded);

        final Accommodation reloaded = repository.findByTripId(tripId).orElseThrow();
        assertThat(reloaded.assignments()).isEmpty();
    }
}
