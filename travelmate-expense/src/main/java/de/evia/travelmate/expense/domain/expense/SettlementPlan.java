package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SettlementPlan(List<Transfer> transfers) {

    public SettlementPlan {
        transfers = List.copyOf(transfers);
    }

    public static SettlementPlan from(final Map<UUID, BigDecimal> balances) {
        final List<BalanceEntry> debtors = new ArrayList<>();
        final List<BalanceEntry> creditors = new ArrayList<>();

        for (final Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            final BigDecimal rounded = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (rounded.signum() < 0) {
                debtors.add(new BalanceEntry(entry.getKey(), rounded.negate()));
            } else if (rounded.signum() > 0) {
                creditors.add(new BalanceEntry(entry.getKey(), rounded));
            }
        }

        debtors.sort(Comparator.comparing(BalanceEntry::remaining).reversed());
        creditors.sort(Comparator.comparing(BalanceEntry::remaining).reversed());

        final List<Transfer> transfers = new ArrayList<>();
        int di = 0;
        int ci = 0;

        while (di < debtors.size() && ci < creditors.size()) {
            final BalanceEntry debtor = debtors.get(di);
            final BalanceEntry creditor = creditors.get(ci);
            final BigDecimal transferAmount = debtor.remaining().min(creditor.remaining());

            if (transferAmount.signum() > 0) {
                transfers.add(new Transfer(debtor.participantId(), creditor.participantId(), transferAmount));
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

        return new SettlementPlan(transfers);
    }

    public BigDecimal totalAmount() {
        return transfers.stream()
            .map(Transfer::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final class BalanceEntry {
        private final UUID participantId;
        private BigDecimal remaining;

        BalanceEntry(final UUID participantId, final BigDecimal remaining) {
            this.participantId = participantId;
            this.remaining = remaining;
        }

        UUID participantId() { return participantId; }
        BigDecimal remaining() { return remaining; }

        void subtract(final BigDecimal amount) {
            this.remaining = this.remaining.subtract(amount);
        }
    }
}
