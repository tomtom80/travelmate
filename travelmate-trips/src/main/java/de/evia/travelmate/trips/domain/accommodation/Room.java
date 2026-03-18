package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public class Room {

    private final RoomId roomId;
    private String name;
    private RoomType roomType;
    private int bedCount;
    private BigDecimal pricePerNight;

    public Room(final RoomId roomId,
                final String name,
                final RoomType roomType,
                final int bedCount,
                final BigDecimal pricePerNight) {
        argumentIsNotNull(roomId, "roomId");
        argumentIsNotBlank(name, "room name");
        argumentIsTrue(name.length() <= 100, "Room name must not exceed 100 characters.");
        argumentIsNotNull(roomType, "roomType");
        argumentIsTrue(bedCount >= 1, "Bed count must be at least 1.");
        if (pricePerNight != null) {
            argumentIsTrue(pricePerNight.compareTo(BigDecimal.ZERO) >= 0,
                "Price per night must be 0 or positive.");
        }
        this.roomId = roomId;
        this.name = name;
        this.roomType = roomType;
        this.bedCount = bedCount;
        this.pricePerNight = pricePerNight;
    }

    public Room(final String name,
                final RoomType roomType,
                final int bedCount,
                final BigDecimal pricePerNight) {
        this(new RoomId(UUID.randomUUID()), name, roomType, bedCount, pricePerNight);
    }

    public RoomId roomId() {
        return roomId;
    }

    public String name() {
        return name;
    }

    public RoomType roomType() {
        return roomType;
    }

    public int bedCount() {
        return bedCount;
    }

    public BigDecimal pricePerNight() {
        return pricePerNight;
    }
}
