package de.evia.travelmate.expense.adapters.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
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
@Table(name = "weighting")
@IdClass(WeightingJpaEntity.WeightingId.class)
public class WeightingJpaEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private ExpenseJpaEntity expense;

    @Id
    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "weight", nullable = false)
    private BigDecimal weight;

    protected WeightingJpaEntity() {
    }

    public WeightingJpaEntity(final ExpenseJpaEntity expense, final UUID participantId,
                              final BigDecimal weight) {
        this.expense = expense;
        this.participantId = participantId;
        this.weight = weight;
    }

    public ExpenseJpaEntity getExpense() { return expense; }
    public UUID getParticipantId() { return participantId; }
    public BigDecimal getWeight() { return weight; }

    public static class WeightingId implements Serializable {
        private UUID expense;
        private UUID participantId;

        public WeightingId() {
        }

        public WeightingId(final UUID expense, final UUID participantId) {
            this.expense = expense;
            this.participantId = participantId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            final WeightingId that = (WeightingId) o;
            return Objects.equals(expense, that.expense)
                && Objects.equals(participantId, that.participantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expense, participantId);
        }
    }
}
