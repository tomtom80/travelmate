package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public class AdvancePayment {

    private final AdvancePaymentId advancePaymentId;
    private final UUID partyTenantId;
    private final String partyName;
    private final BigDecimal amount;
    private boolean paid;

    public AdvancePayment(final AdvancePaymentId advancePaymentId,
                          final UUID partyTenantId,
                          final String partyName,
                          final BigDecimal amount,
                          final boolean paid) {
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
    }

    public void togglePaid() {
        this.paid = !this.paid;
    }

    public AdvancePaymentId advancePaymentId() { return advancePaymentId; }
    public UUID partyTenantId() { return partyTenantId; }
    public String partyName() { return partyName; }
    public BigDecimal amount() { return amount; }
    public boolean paid() { return paid; }
}
