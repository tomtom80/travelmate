package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class AdvancePayment {

    private final AdvancePaymentId advancePaymentId;
    private final UUID partyTenantId;
    private final String partyName;
    private final BigDecimal amount;
    private boolean paid;
    private LocalDate paidOn;
    private UUID markedByParticipantId;

    public AdvancePayment(final AdvancePaymentId advancePaymentId,
                          final UUID partyTenantId,
                          final String partyName,
                          final BigDecimal amount,
                          final boolean paid,
                          final LocalDate paidOn,
                          final UUID markedByParticipantId) {
        argumentIsNotNull(advancePaymentId, "advancePaymentId");
        argumentIsNotNull(partyTenantId, "partyTenantId");
        argumentIsNotNull(partyName, "partyName");
        argumentIsNotNull(amount, "amount");
        argumentIsTrue(amount.compareTo(BigDecimal.ZERO) > 0,
            "Advance payment amount must be greater than 0.");
        this.advancePaymentId = advancePaymentId;
        this.partyTenantId = partyTenantId;
        this.partyName = partyName;
        this.amount = amount;
        this.paid = paid;
        this.paidOn = paid ? paidOn : null;
        this.markedByParticipantId = paid ? markedByParticipantId : null;
    }

    public AdvancePayment(final AdvancePaymentId advancePaymentId,
                          final UUID partyTenantId,
                          final String partyName,
                          final BigDecimal amount,
                          final boolean paid) {
        this(advancePaymentId, partyTenantId, partyName, amount, paid, null, null);
    }

    public void togglePaid(final UUID markedByParticipantId) {
        this.paid = !this.paid;
        this.paidOn = this.paid ? LocalDate.now() : null;
        this.markedByParticipantId = this.paid ? markedByParticipantId : null;
    }

    public AdvancePaymentId advancePaymentId() { return advancePaymentId; }
    public UUID partyTenantId() { return partyTenantId; }
    public String partyName() { return partyName; }
    public BigDecimal amount() { return amount; }
    public boolean paid() { return paid; }
    public LocalDate paidOn() { return paidOn; }
    public UUID markedByParticipantId() { return markedByParticipantId; }
}
