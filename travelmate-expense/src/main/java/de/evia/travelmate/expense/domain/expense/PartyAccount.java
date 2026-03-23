package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

public record PartyAccount(
    UUID partyTenantId,
    String partyName,
    List<String> memberNames,
    List<PartyAccountEntry> entries,
    BigDecimal receiptCredits,
    BigDecimal advancePaymentsPlanned,
    BigDecimal advancePaymentsPaid,
    BigDecimal advancePaymentsOutstanding,
    BigDecimal fairShare,
    BigDecimal currentBalance,
    BigDecimal outstandingAmount,
    BigDecimal creditAmount
) {
    public PartyAccount {
        argumentIsNotNull(partyTenantId, "partyTenantId");
        argumentIsNotNull(partyName, "partyName");
        argumentIsNotNull(memberNames, "memberNames");
        argumentIsNotNull(entries, "entries");
        argumentIsNotNull(receiptCredits, "receiptCredits");
        argumentIsNotNull(advancePaymentsPlanned, "advancePaymentsPlanned");
        argumentIsNotNull(advancePaymentsPaid, "advancePaymentsPaid");
        argumentIsNotNull(advancePaymentsOutstanding, "advancePaymentsOutstanding");
        argumentIsNotNull(fairShare, "fairShare");
        argumentIsNotNull(currentBalance, "currentBalance");
        argumentIsNotNull(outstandingAmount, "outstandingAmount");
        argumentIsNotNull(creditAmount, "creditAmount");
    }
}
