package de.evia.travelmate.expense.adapters.persistence;

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
@Table(name = "advance_payment")
public class AdvancePaymentJpaEntity {

    @Id
    @Column(name = "advance_payment_id")
    private UUID advancePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private ExpenseJpaEntity expense;

    @Column(name = "party_tenant_id", nullable = false)
    private UUID partyTenantId;

    @Column(name = "party_name", nullable = false, length = 200)
    private String partyName;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    protected AdvancePaymentJpaEntity() {
    }

    public AdvancePaymentJpaEntity(final UUID advancePaymentId,
                                   final ExpenseJpaEntity expense,
                                   final UUID partyTenantId,
                                   final String partyName,
                                   final BigDecimal amount,
                                   final boolean paid) {
        this.advancePaymentId = advancePaymentId;
        this.expense = expense;
        this.partyTenantId = partyTenantId;
        this.partyName = partyName;
        this.amount = amount;
        this.paid = paid;
    }

    public UUID getAdvancePaymentId() { return advancePaymentId; }
    public ExpenseJpaEntity getExpense() { return expense; }
    public UUID getPartyTenantId() { return partyTenantId; }
    public String getPartyName() { return partyName; }
    public BigDecimal getAmount() { return amount; }
    public boolean isPaid() { return paid; }
    public void setPaid(final boolean paid) { this.paid = paid; }
    public void setAmount(final BigDecimal amount) { this.amount = amount; }
    public void setPartyName(final String partyName) { this.partyName = partyName; }
}
