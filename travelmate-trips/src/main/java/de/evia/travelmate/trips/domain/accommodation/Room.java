package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public class Room {

    private final RoomId roomId;
    private String name;
    private int bedCount;
    private BigDecimal pricePerNight;

    public Room(final RoomId roomId,
                final String name,
                final int bedCount,
                final BigDecimal pricePerNight) {
        argumentIsNotNull(roomId, "roomId");
        argumentIsNotBlank(name, "room name");
        argumentIsTrue(name.length() <= 100, "Room name must not exceed 100 characters.");
        argumentIsTrue(bedCount >= 1, "Bed count must be at least 1.");
        if (pricePerNight != null) {
            argumentIsTrue(pricePerNight.compareTo(BigDecimal.ZERO) >= 0,
                "Price per night must be 0 or positive.");
        }
        this.roomId = roomId;
        this.name = name;
        this.bedCount = bedCount;
        this.pricePerNight = pricePerNight;
    }

    public Room(final String name,
                final int bedCount,
                final BigDecimal pricePerNight) {
        this(new RoomId(UUID.randomUUID()), name, bedCount, pricePerNight);
    }

    public void update(final String name, final int bedCount) {
        argumentIsNotBlank(name, "room name");
        argumentIsTrue(name.length() <= 100, "Room name must not exceed 100 characters.");
        argumentIsTrue(bedCount >= 1, "Bed count must be at least 1.");
        this.name = name;
        this.bedCount = bedCount;
    }

    public RoomId roomId() {
        return roomId;
    }

    public String name() {
        return name;
    }

    public int bedCount() {
        return bedCount;
    }

    public BigDecimal pricePerNight() {
        return pricePerNight;
    }
}
