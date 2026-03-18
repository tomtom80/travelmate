package de.evia.travelmate.expense.adapters.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_participant")
@IdClass(TripParticipantJpaEntity.TripParticipantId.class)
public class TripParticipantJpaEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private TripProjectionJpaEntity tripProjection;

    @Id
    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "party_tenant_id")
    private UUID partyTenantId;

    @Column(name = "party_name")
    private String partyName;

    protected TripParticipantJpaEntity() {
    }

    public TripParticipantJpaEntity(final TripProjectionJpaEntity tripProjection,
                                    final UUID participantId, final String name) {
        this.tripProjection = tripProjection;
        this.participantId = participantId;
        this.name = name;
    }

    public TripProjectionJpaEntity getTripProjection() { return tripProjection; }
    public UUID getParticipantId() { return participantId; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(final LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(final LocalDate departureDate) { this.departureDate = departureDate; }
    public UUID getPartyTenantId() { return partyTenantId; }
    public void setPartyTenantId(final UUID partyTenantId) { this.partyTenantId = partyTenantId; }
    public String getPartyName() { return partyName; }
    public void setPartyName(final String partyName) { this.partyName = partyName; }

    public static class TripParticipantId implements Serializable {
        private UUID tripProjection;
        private UUID participantId;

        public TripParticipantId() {
        }

        public TripParticipantId(final UUID tripProjection, final UUID participantId) {
            this.tripProjection = tripProjection;
            this.participantId = participantId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            final TripParticipantId that = (TripParticipantId) o;
            return Objects.equals(tripProjection, that.tripProjection)
                && Objects.equals(participantId, that.participantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tripProjection, participantId);
        }
    }
}
