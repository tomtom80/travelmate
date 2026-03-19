package de.evia.travelmate.trips.domain.accommodation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.trips.domain.trip.TripId;

class AccommodationTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());

    @Test
    void createAccommodationWithOneRoom() {
        final Room room = new Room("Zimmer 1", 2, new BigDecimal("80.00"));

        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Berghuette Alpenblick", "Alpweg 12", "https://booking.com/123",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
            new BigDecimal("320.00"), List.of(room)
        );

        assertThat(accommodation.accommodationId()).isNotNull();
        assertThat(accommodation.tenantId()).isEqualTo(TENANT_ID);
        assertThat(accommodation.tripId()).isEqualTo(TRIP_ID);
        assertThat(accommodation.name()).isEqualTo("Berghuette Alpenblick");
        assertThat(accommodation.address()).isEqualTo("Alpweg 12");
        assertThat(accommodation.url()).isEqualTo("https://booking.com/123");
        assertThat(accommodation.checkIn()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(accommodation.checkOut()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(accommodation.totalPrice()).isEqualByComparingTo(new BigDecimal("320.00"));
        assertThat(accommodation.rooms()).hasSize(1);
    }

    @Test
    void createRegistersAccommodationPriceSetEvent() {
        final Room room = new Room("Zimmer 1", 2, null);

        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, new BigDecimal("3000.00"), List.of(room)
        );

        assertThat(accommodation.domainEvents()).hasSize(1);
        assertThat(accommodation.domainEvents().getFirst()).isInstanceOf(AccommodationPriceSet.class);
        final AccommodationPriceSet event = (AccommodationPriceSet) accommodation.domainEvents().getFirst();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID.value());
        assertThat(event.tripId()).isEqualTo(TRIP_ID.value());
        assertThat(event.totalPrice()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void createWithNullPriceDoesNotRegisterEvent() {
        final Room room = new Room("Zimmer 1", 2, null);

        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThat(accommodation.domainEvents()).isEmpty();
    }

    @Test
    void createWithZeroPriceDoesNotRegisterEvent() {
        final Room room = new Room("Zimmer 1", 2, null);

        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, BigDecimal.ZERO, List.of(room)
        );

        assertThat(accommodation.domainEvents()).isEmpty();
    }

    @Test
    void createRejectsEmptyRoomList() {
        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one room");
    }

    @Test
    void createRejectsNullRoomList() {
        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one room");
    }

    @Test
    void createRejectsBlankName() {
        final Room room = new Room("Zimmer 1", 2, null);

        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, "", null, null,
            null, null, null, List.of(room)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNameExceeding200Characters() {
        final Room room = new Room("Zimmer 1", 2, null);
        final String longName = "A".repeat(201);

        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, longName, null, null,
            null, null, null, List.of(room)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("200 characters");
    }

    @Test
    void createRejectsCheckInAfterCheckOut() {
        final Room room = new Room("Zimmer 1", 2, null);

        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 1),
            null, List.of(room)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Check-in date must be before check-out date");
    }

    @Test
    void createRejectsNegativeTotalPrice() {
        final Room room = new Room("Zimmer 1", 2, null);

        assertThatThrownBy(() -> Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, new BigDecimal("-100"), List.of(room)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Total price must be 0 or positive");
    }

    @Test
    void addRoom() {
        final Room room1 = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room1)
        );
        final Room room2 = new Room("Zimmer 2", 4, null);

        accommodation.addRoom(room2);

        assertThat(accommodation.rooms()).hasSize(2);
    }

    @Test
    void removeRoom() {
        final Room room1 = new Room("Zimmer 1", 2, null);
        final Room room2 = new Room("Zimmer 2", 4, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room1, room2)
        );

        accommodation.removeRoom(room1.roomId());

        assertThat(accommodation.rooms()).hasSize(1);
        assertThat(accommodation.rooms().getFirst().roomId()).isEqualTo(room2.roomId());
    }

    @Test
    void removeLastRoomIsRejected() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.removeRoom(room.roomId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one room");
    }

    @Test
    void removeNonExistentRoomIsRejected() {
        final Room room1 = new Room("Zimmer 1", 2, null);
        final Room room2 = new Room("Zimmer 2", 4, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room1, room2)
        );

        assertThatThrownBy(() -> accommodation.removeRoom(new RoomId(UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void totalBedCount() {
        final Room room1 = new Room("Zimmer 1", 2, null);
        final Room room2 = new Room("Zimmer 2", 4, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room1, room2)
        );

        assertThat(accommodation.totalBedCount()).isEqualTo(6);
    }

    @Test
    void updateDetailsChangesFields() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );
        accommodation.clearDomainEvents();

        accommodation.updateDetails(
            "Neue Huette", "Neuer Weg 1", "https://new.com",
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
            new BigDecimal("500.00")
        );

        assertThat(accommodation.name()).isEqualTo("Neue Huette");
        assertThat(accommodation.address()).isEqualTo("Neuer Weg 1");
        assertThat(accommodation.url()).isEqualTo("https://new.com");
        assertThat(accommodation.checkIn()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(accommodation.checkOut()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(accommodation.totalPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void updateDetailsPublishesPriceSetEventWhenPriceChanges() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, new BigDecimal("100.00"), List.of(room)
        );
        accommodation.clearDomainEvents();

        accommodation.updateDetails("Huette", null, null, null, null, new BigDecimal("200.00"));

        assertThat(accommodation.domainEvents()).hasSize(1);
        final AccommodationPriceSet event = (AccommodationPriceSet) accommodation.domainEvents().getFirst();
        assertThat(event.totalPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void updateDetailsDoesNotPublishEventWhenPriceUnchanged() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, new BigDecimal("100.00"), List.of(room)
        );
        accommodation.clearDomainEvents();

        accommodation.updateDetails("Huette Updated", null, null, null, null, new BigDecimal("100.00"));

        assertThat(accommodation.domainEvents()).isEmpty();
    }

    @Test
    void assignPartyToRoom() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );
        final UUID partyId = UUID.randomUUID();

        accommodation.assignPartyToRoom(room.roomId(), partyId, "Familie Schmidt", 2);

        assertThat(accommodation.assignments()).hasSize(1);
        assertThat(accommodation.assignments().getFirst().partyName()).isEqualTo("Familie Schmidt");
        assertThat(accommodation.assignments().getFirst().personCount()).isEqualTo(2);
        assertThat(accommodation.totalAssignedPersons()).isEqualTo(2);
    }

    @Test
    void assignMultiplePartiesToSameRoom() {
        final Room room = new Room("Matratzenlager", 10, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        accommodation.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Familie A", 3);
        accommodation.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Familie B", 4);

        assertThat(accommodation.assignments()).hasSize(2);
        assertThat(accommodation.totalAssignedPersonsForRoom(room.roomId())).isEqualTo(7);
    }

    @Test
    void assignPartyToRoomRejectsInvalidRoomId() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.assignPartyToRoom(
            new RoomId(UUID.randomUUID()), UUID.randomUUID(), "Test", 1
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void assignPartyToRoomRejectsZeroPersonCount() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.assignPartyToRoom(
            room.roomId(), UUID.randomUUID(), "Test", 0
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Person count must be at least 1");
    }

    @Test
    void removeAssignment() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );
        accommodation.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Familie Schmidt", 2);
        final var assignmentId = accommodation.assignments().getFirst().assignmentId();

        accommodation.removeAssignment(assignmentId);

        assertThat(accommodation.assignments()).isEmpty();
    }

    @Test
    void removeAssignmentRejectsInvalidId() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.removeAssignment(
            new RoomAssignmentId(UUID.randomUUID())
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void updateAssignmentPersonCount() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );
        accommodation.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Familie Schmidt", 2);
        final var assignmentId = accommodation.assignments().getFirst().assignmentId();

        accommodation.updateAssignmentPersonCount(assignmentId, 3);

        assertThat(accommodation.assignments().getFirst().personCount()).isEqualTo(3);
    }

    @Test
    void assignmentsForRoom() {
        final Room room1 = new Room("Zimmer 1", 2, null);
        final Room room2 = new Room("Zimmer 2", 4, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room1, room2)
        );
        accommodation.assignPartyToRoom(room1.roomId(), UUID.randomUUID(), "Familie A", 2);
        accommodation.assignPartyToRoom(room2.roomId(), UUID.randomUUID(), "Familie B", 3);

        assertThat(accommodation.assignmentsForRoom(room1.roomId())).hasSize(1);
        assertThat(accommodation.assignmentsForRoom(room2.roomId())).hasSize(1);
    }

    @Test
    void personCountExceedingBedCountIsAllowed() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        accommodation.assignPartyToRoom(room.roomId(), UUID.randomUUID(), "Grosse Familie", 5);

        assertThat(accommodation.totalAssignedPersonsForRoom(room.roomId())).isEqualTo(5);
    }

    @Test
    void updateRoomChangesNameAndBedCount() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        accommodation.updateRoom(room.roomId(), "Grosses Zimmer", 4);

        assertThat(accommodation.rooms().getFirst().name()).isEqualTo("Grosses Zimmer");
        assertThat(accommodation.rooms().getFirst().bedCount()).isEqualTo(4);
    }

    @Test
    void updateRoomRejectsInvalidRoomId() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.updateRoom(
            new RoomId(UUID.randomUUID()), "Name", 2
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void updateRoomRejectsBlankName() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.updateRoom(room.roomId(), "", 2))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRoomRejectsZeroBedCount() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThatThrownBy(() -> accommodation.updateRoom(room.roomId(), "Zimmer 1", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Bed count must be at least 1");
    }

    @Test
    void createAccommodationHasEmptyAssignments() {
        final Room room = new Room("Zimmer 1", 2, null);
        final Accommodation accommodation = Accommodation.create(
            TENANT_ID, TRIP_ID, "Huette", null, null,
            null, null, null, List.of(room)
        );

        assertThat(accommodation.assignments()).isEmpty();
        assertThat(accommodation.totalAssignedPersons()).isEqualTo(0);
    }
}
