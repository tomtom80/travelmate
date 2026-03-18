package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes party-level balances and transfers by aggregating individual participant
 * balances grouped by their TravelParty (identified by partyTenantId).
 */
public final class PartySettlement {

    private PartySettlement() {
    }

    /**
     * Groups individual balances by partyTenantId. Returns a map from partyTenantId to the
     * aggregated balance for that party. Only participants with a non-null partyTenantId are
     * included.
     */
    public static Map<UUID, BigDecimal> aggregateByParty(
            final Map<UUID, BigDecimal> individualBalances,
            final Map<UUID, UUID> participantToParty) {
        final Map<UUID, BigDecimal> partyBalances = new LinkedHashMap<>();
        for (final Map.Entry<UUID, BigDecimal> entry : individualBalances.entrySet()) {
            final UUID participantId = entry.getKey();
            final UUID partyId = participantToParty.get(participantId);
            if (partyId != null) {
                partyBalances.merge(partyId, entry.getValue(), BigDecimal::add);
            }
        }
        return partyBalances;
    }

    /**
     * Generates the minimum set of party-level transfers to settle all debts.
     * Uses the same greedy algorithm as {@link SettlementPlan}.
     */
    public static List<PartyTransfer> calculateTransfers(final Map<UUID, BigDecimal> partyBalances) {
        final List<BalanceEntry> debtors = new ArrayList<>();
        final List<BalanceEntry> creditors = new ArrayList<>();

        for (final Map.Entry<UUID, BigDecimal> entry : partyBalances.entrySet()) {
            final BigDecimal rounded = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (rounded.signum() < 0) {
                debtors.add(new BalanceEntry(entry.getKey(), rounded.negate()));
            } else if (rounded.signum() > 0) {
                creditors.add(new BalanceEntry(entry.getKey(), rounded));
            }
        }

        debtors.sort(Comparator.comparing(BalanceEntry::remaining).reversed());
        creditors.sort(Comparator.comparing(BalanceEntry::remaining).reversed());

        final List<PartyTransfer> transfers = new ArrayList<>();
        int di = 0;
        int ci = 0;

        while (di < debtors.size() && ci < creditors.size()) {
            final BalanceEntry debtor = debtors.get(di);
            final BalanceEntry creditor = creditors.get(ci);
            final BigDecimal transferAmount = debtor.remaining().min(creditor.remaining());

            if (transferAmount.signum() > 0) {
                transfers.add(new PartyTransfer(debtor.partyId(), creditor.partyId(), transferAmount));
            }

            debtor.subtract(transferAmount);
            creditor.subtract(transferAmount);

            if (debtor.remaining().signum() == 0) {
                di++;
            }
            if (creditor.remaining().signum() == 0) {
                ci++;
            }
        }

        return transfers;
    }

    public record PartyTransfer(UUID fromPartyId, UUID toPartyId, BigDecimal amount) {
    }

    private static final class BalanceEntry {
        private final UUID partyId;
        private BigDecimal remaining;

        BalanceEntry(final UUID partyId, final BigDecimal remaining) {
            this.partyId = partyId;
            this.remaining = remaining;
        }

        UUID partyId() { return partyId; }
        BigDecimal remaining() { return remaining; }

        void subtract(final BigDecimal amount) {
            this.remaining = this.remaining.subtract(amount);
        }
    }
}
