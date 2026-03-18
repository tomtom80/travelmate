package de.evia.travelmate.trips.adapters.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodation_room")
public class AccommodationRoomJpaEntity {

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private AccommodationJpaEntity accommodation;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    @Column(name = "bed_count", nullable = false)
    private int bedCount;

    @Column(name = "price_per_night")
    private BigDecimal pricePerNight;

    protected AccommodationRoomJpaEntity() {
    }

    public AccommodationRoomJpaEntity(final UUID roomId, final AccommodationJpaEntity accommodation,
                                       final String name, final String roomType,
                                       final int bedCount, final BigDecimal pricePerNight) {
        this.roomId = roomId;
        this.accommodation = accommodation;
        this.name = name;
        this.roomType = roomType;
        this.bedCount = bedCount;
        this.pricePerNight = pricePerNight;
    }

    public UUID getRoomId() { return roomId; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public String getRoomType() { return roomType; }
    public void setRoomType(final String roomType) { this.roomType = roomType; }
    public int getBedCount() { return bedCount; }
    public void setBedCount(final int bedCount) { this.bedCount = bedCount; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(final BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
}
