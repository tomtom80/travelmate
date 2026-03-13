package de.evia.travelmate.trips.adapters.persistence;

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
@IdClass(ParticipantJpaEntity.ParticipantId.class)
public class ParticipantJpaEntity {

    @Id
    @Column(name = "participant_id")
    private UUID participantId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private TripJpaEntity trip;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    protected ParticipantJpaEntity() {
    }

    public ParticipantJpaEntity(final UUID participantId, final TripJpaEntity trip,
                                final String firstName, final String lastName,
                                final LocalDate arrivalDate, final LocalDate departureDate) {
        this.participantId = participantId;
        this.trip = trip;
        this.firstName = firstName;
        this.lastName = lastName;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    public UUID getParticipantId() { return participantId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(final String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(final String lastName) { this.lastName = lastName; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(final LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(final LocalDate departureDate) { this.departureDate = departureDate; }

    public static class ParticipantId implements Serializable {
        private UUID participantId;
        private UUID trip;

        public ParticipantId() {
        }

        public ParticipantId(final UUID participantId, final UUID trip) {
            this.participantId = participantId;
            this.trip = trip;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            final ParticipantId that = (ParticipantId) o;
            return Objects.equals(participantId, that.participantId) && Objects.equals(trip, that.trip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(participantId, trip);
        }
    }
}
