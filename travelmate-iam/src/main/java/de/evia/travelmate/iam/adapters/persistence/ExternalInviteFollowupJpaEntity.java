package de.evia.travelmate.iam.adapters.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "external_invite_followups")
@IdClass(ExternalInviteFollowupJpaEntity.PK.class)
public class ExternalInviteFollowupJpaEntity {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Id
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    protected ExternalInviteFollowupJpaEntity() {
    }

    public ExternalInviteFollowupJpaEntity(final String email,
                                           final UUID tripId,
                                           final String actionType,
                                           final Instant dispatchedAt) {
        this.email = email;
        this.tripId = tripId;
        this.actionType = actionType;
        this.dispatchedAt = dispatchedAt;
    }

    public String getEmail() {
        return email;
    }

    public UUID getTripId() {
        return tripId;
    }

    public String getActionType() {
        return actionType;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public static class PK implements Serializable {
        private String email;
        private UUID tripId;

        public PK() {
        }

        public PK(final String email, final UUID tripId) {
            this.email = email;
            this.tripId = tripId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PK pk)) {
                return false;
            }
            return Objects.equals(email, pk.email) && Objects.equals(tripId, pk.tripId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email, tripId);
        }
    }
}
