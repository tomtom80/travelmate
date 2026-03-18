package de.evia.travelmate.trips.adapters.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodation")
public class AccommodationJpaEntity {

    @Id
    @Column(name = "accommodation_id")
    private UUID accommodationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false, unique = true)
    private UUID tripId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("name ASC")
    private List<AccommodationRoomJpaEntity> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("assignedAt ASC")
    private List<RoomAssignmentJpaEntity> assignments = new ArrayList<>();

    protected AccommodationJpaEntity() {
    }

    public AccommodationJpaEntity(final UUID accommodationId, final UUID tenantId, final UUID tripId,
                                   final String name, final String address, final String url,
                                   final LocalDate checkIn, final LocalDate checkOut,
                                   final BigDecimal totalPrice) {
        this.accommodationId = accommodationId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.name = name;
        this.address = address;
        this.url = url;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
    }

    public UUID getAccommodationId() { return accommodationId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(final String address) { this.address = address; }
    public String getUrl() { return url; }
    public void setUrl(final String url) { this.url = url; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(final LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(final LocalDate checkOut) { this.checkOut = checkOut; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(final BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public List<AccommodationRoomJpaEntity> getRooms() { return rooms; }
    public List<RoomAssignmentJpaEntity> getAssignments() { return assignments; }
}
