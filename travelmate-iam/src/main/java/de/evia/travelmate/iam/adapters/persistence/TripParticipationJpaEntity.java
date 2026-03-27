package de.evia.travelmate.iam.adapters.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_participation")
@IdClass(TripParticipationJpaEntity.TripParticipationId.class)
public class TripParticipationJpaEntity {

    @Id
    @Column(name = "participant_id")
    private UUID participantId;

    @Id
    @Column(name = "trip_id")
    private UUID tripId;

    protected TripParticipationJpaEntity() {
    }

    public TripParticipationJpaEntity(final UUID participantId, final UUID tripId) {
        this.participantId = participantId;
        this.tripId = tripId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public static class TripParticipationId implements Serializable {
        private UUID participantId;
        private UUID tripId;

        public TripParticipationId() {
        }

        public TripParticipationId(final UUID participantId, final UUID tripId) {
            this.participantId = participantId;
            this.tripId = tripId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TripParticipationId that = (TripParticipationId) o;
            return Objects.equals(participantId, that.participantId)
                && Objects.equals(tripId, that.tripId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(participantId, tripId);
        }
    }
}
