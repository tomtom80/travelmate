package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.trips.domain.trip.TripId;

public class Accommodation extends AggregateRoot {

    private final AccommodationId accommodationId;
    private final TenantId tenantId;
    private final TripId tripId;
    private String name;
    private String address;
    private String url;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal totalPrice;
    private final List<Room> rooms;
    private final List<RoomAssignment> assignments;

    public Accommodation(final AccommodationId accommodationId,
                         final TenantId tenantId,
                         final TripId tripId,
                         final String name,
                         final String address,
                         final String url,
                         final LocalDate checkIn,
                         final LocalDate checkOut,
                         final BigDecimal totalPrice,
                         final List<Room> rooms,
                         final List<RoomAssignment> assignments) {
        argumentIsNotNull(accommodationId, "accommodationId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotBlank(name, "accommodation name");
        argumentIsTrue(name.length() <= 200, "Accommodation name must not exceed 200 characters.");
        if (address != null) {
            argumentIsTrue(address.length() <= 500, "Address must not exceed 500 characters.");
        }
        if (url != null) {
            argumentIsTrue(url.length() <= 1000, "URL must not exceed 1000 characters.");
        }
        if (checkIn != null && checkOut != null) {
            argumentIsTrue(checkIn.isBefore(checkOut),
                "Check-in date must be before check-out date.");
        }
        if (totalPrice != null) {
            argumentIsTrue(totalPrice.compareTo(BigDecimal.ZERO) >= 0,
                "Total price must be 0 or positive.");
        }
        this.accommodationId = accommodationId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.name = name;
        this.address = address;
        this.url = url;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.rooms = new ArrayList<>(rooms);
        this.assignments = new ArrayList<>(assignments != null ? assignments : List.of());
    }

    public static Accommodation create(final TenantId tenantId,
                                        final TripId tripId,
                                        final String name,
                                        final String address,
                                        final String url,
                                        final LocalDate checkIn,
                                        final LocalDate checkOut,
                                        final BigDecimal totalPrice,
                                        final List<Room> rooms) {
        argumentIsTrue(rooms != null && !rooms.isEmpty(),
            "Accommodation must have at least one room.");
        final Accommodation accommodation = new Accommodation(
            new AccommodationId(UUID.randomUUID()),
            tenantId, tripId, name, address, url,
            checkIn, checkOut, totalPrice, rooms, List.of()
        );
        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            accommodation.registerEvent(new AccommodationPriceSet(
                tenantId.value(), tripId.value(), totalPrice, LocalDate.now()
            ));
        }
        return accommodation;
    }

    public void updateDetails(final String name,
                              final String address,
                              final String url,
                              final LocalDate checkIn,
                              final LocalDate checkOut,
                              final BigDecimal totalPrice) {
        argumentIsNotBlank(name, "accommodation name");
        argumentIsTrue(name.length() <= 200, "Accommodation name must not exceed 200 characters.");
        if (address != null) {
            argumentIsTrue(address.length() <= 500, "Address must not exceed 500 characters.");
        }
        if (url != null) {
            argumentIsTrue(url.length() <= 1000, "URL must not exceed 1000 characters.");
        }
        if (checkIn != null && checkOut != null) {
            argumentIsTrue(checkIn.isBefore(checkOut),
                "Check-in date must be before check-out date.");
        }
        if (totalPrice != null) {
            argumentIsTrue(totalPrice.compareTo(BigDecimal.ZERO) >= 0,
                "Total price must be 0 or positive.");
        }
        final BigDecimal oldPrice = this.totalPrice;
        this.name = name;
        this.address = address;
        this.url = url;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (oldPrice == null || totalPrice.compareTo(oldPrice) != 0) {
                registerEvent(new AccommodationPriceSet(
                    tenantId.value(), tripId.value(), totalPrice, LocalDate.now()
                ));
            }
        }
    }

    public void addRoom(final Room room) {
        argumentIsNotNull(room, "room");
        rooms.add(room);
    }

    public void removeRoom(final RoomId roomId) {
        argumentIsNotNull(roomId, "roomId");
        argumentIsTrue(rooms.size() > 1,
            "Accommodation must have at least one room.");
        final boolean removed = rooms.removeIf(r -> r.roomId().equals(roomId));
        argumentIsTrue(removed, "Room " + roomId.value() + " not found in this accommodation.");
    }

    public AccommodationId accommodationId() {
        return accommodationId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    public String url() {
        return url;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public BigDecimal totalPrice() {
        return totalPrice;
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    public int totalBedCount() {
        return rooms.stream().mapToInt(Room::bedCount).sum();
    }

    public List<RoomAssignment> assignments() {
        return Collections.unmodifiableList(assignments);
    }

    public RoomAssignment assignPartyToRoom(final RoomId roomId,
                                             final UUID partyTenantId,
                                             final String partyName,
                                             final int personCount) {
        argumentIsNotNull(roomId, "roomId");
        argumentIsNotNull(partyTenantId, "partyTenantId");
        argumentIsNotBlank(partyName, "partyName");
        argumentIsTrue(personCount > 0, "Person count must be at least 1.");
        argumentIsTrue(rooms.stream().anyMatch(r -> r.roomId().equals(roomId)),
            "Room " + roomId.value() + " not found in this accommodation.");
        final RoomAssignment assignment = RoomAssignment.create(roomId, partyTenantId, partyName, personCount);
        assignments.add(assignment);
        return assignment;
    }

    public void removeAssignment(final RoomAssignmentId assignmentId) {
        argumentIsNotNull(assignmentId, "assignmentId");
        final boolean removed = assignments.removeIf(a -> a.assignmentId().equals(assignmentId));
        argumentIsTrue(removed, "Assignment " + assignmentId.value() + " not found in this accommodation.");
    }

    public void updateAssignmentPersonCount(final RoomAssignmentId assignmentId, final int personCount) {
        argumentIsNotNull(assignmentId, "assignmentId");
        final RoomAssignment assignment = assignments.stream()
            .filter(a -> a.assignmentId().equals(assignmentId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Assignment " + assignmentId.value() + " not found in this accommodation."));
        assignment.updatePersonCount(personCount);
    }

    public List<RoomAssignment> assignmentsForRoom(final RoomId roomId) {
        return assignments.stream()
            .filter(a -> a.roomId().equals(roomId))
            .toList();
    }

    public int totalAssignedPersonsForRoom(final RoomId roomId) {
        return assignments.stream()
            .filter(a -> a.roomId().equals(roomId))
            .mapToInt(RoomAssignment::personCount)
            .sum();
    }

    public int totalAssignedPersons() {
        return assignments.stream().mapToInt(RoomAssignment::personCount).sum();
    }
}
