package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.expense.ExpenseCreated;
import de.evia.travelmate.common.events.expense.ExpenseSettled;

public class Expense extends AggregateRoot {

    private final ExpenseId expenseId;
    private final TenantId tenantId;
    private final UUID tripId;
    private final List<Receipt> receipts;
    private final List<ParticipantWeighting> weightings;
    private ExpenseStatus status;

    public Expense(final ExpenseId expenseId,
                   final TenantId tenantId,
                   final UUID tripId,
                   final ExpenseStatus status,
                   final List<Receipt> receipts,
                   final List<ParticipantWeighting> weightings) {
        argumentIsNotNull(expenseId, "expenseId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(status, "status");
        argumentIsNotNull(weightings, "weightings");
        this.expenseId = expenseId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
        this.receipts = new ArrayList<>(receipts);
        this.weightings = new ArrayList<>(weightings);
    }

    public static Expense create(final TenantId tenantId,
                                 final UUID tripId,
                                 final List<ParticipantWeighting> weightings) {
        argumentIsTrue(!weightings.isEmpty(), "At least one participant weighting is required.");
        final Expense expense = new Expense(
            new ExpenseId(UUID.randomUUID()),
            tenantId,
            tripId,
            ExpenseStatus.OPEN,
            List.of(),
            weightings
        );
        expense.registerEvent(new ExpenseCreated(
            tenantId.value(), tripId, expense.expenseId.value(), LocalDate.now()
        ));
        return expense;
    }

    public void addReceipt(final String description, final Amount amount,
                           final UUID paidBy, final LocalDate date) {
        assertOpen();
        argumentIsTrue(hasParticipant(paidBy),
            "Payer " + paidBy + " is not a participant in this expense.");
        final Receipt receipt = new Receipt(
            new ReceiptId(UUID.randomUUID()), description, amount, paidBy, date
        );
        receipts.add(receipt);
    }

    public void removeReceipt(final ReceiptId receiptId) {
        assertOpen();
        final boolean removed = receipts.removeIf(
            r -> r.receiptId().equals(receiptId));
        argumentIsTrue(removed, "Receipt " + receiptId.value() + " not found.");
    }

    public void updateWeighting(final UUID participantId, final BigDecimal newWeight) {
        assertOpen();
        argumentIsNotNull(newWeight, "weight");
        argumentIsTrue(newWeight.compareTo(BigDecimal.ZERO) >= 0,
            "Weight must not be negative.");
        weightings.removeIf(w -> w.participantId().equals(participantId));
        weightings.add(new ParticipantWeighting(participantId, newWeight));
    }

    public void settle() {
        assertOpen();
        argumentIsTrue(!receipts.isEmpty(), "Cannot settle an expense with no receipts.");
        this.status = ExpenseStatus.SETTLED;
        registerEvent(new ExpenseSettled(
            tenantId.value(), tripId, expenseId.value(), LocalDate.now()
        ));
    }

    /**
     * Calculates the net balance for each participant.
     * Positive = others owe this participant. Negative = this participant owes others.
     */
    public Map<UUID, BigDecimal> calculateBalances() {
        final BigDecimal totalWeight = weightings.stream()
            .map(ParticipantWeighting::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return Map.of();
        }

        final Map<UUID, BigDecimal> paid = new HashMap<>();
        final Map<UUID, BigDecimal> owed = new HashMap<>();

        for (final ParticipantWeighting w : weightings) {
            paid.put(w.participantId(), BigDecimal.ZERO);
            owed.put(w.participantId(), BigDecimal.ZERO);
        }

        for (final Receipt receipt : receipts) {
            final BigDecimal receiptAmount = receipt.amount().value();
            paid.merge(receipt.paidBy(), receiptAmount, BigDecimal::add);

            for (final ParticipantWeighting w : weightings) {
                final BigDecimal share = receiptAmount
                    .multiply(w.weight())
                    .divide(totalWeight, 2, RoundingMode.HALF_UP);
                owed.merge(w.participantId(), share, BigDecimal::add);
            }
        }

        final Map<UUID, BigDecimal> balances = new HashMap<>();
        for (final ParticipantWeighting w : weightings) {
            final UUID pid = w.participantId();
            final BigDecimal balance = paid.getOrDefault(pid, BigDecimal.ZERO)
                .subtract(owed.getOrDefault(pid, BigDecimal.ZERO));
            balances.put(pid, balance);
        }
        return Collections.unmodifiableMap(balances);
    }

    private boolean hasParticipant(final UUID participantId) {
        return weightings.stream()
            .anyMatch(w -> w.participantId().equals(participantId));
    }

    private void assertOpen() {
        if (status != ExpenseStatus.OPEN) {
            throw new IllegalStateException("Expense is " + status + ", modifications not allowed.");
        }
    }

    public ExpenseId expenseId() {
        return expenseId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public UUID tripId() {
        return tripId;
    }

    public ExpenseStatus status() {
        return status;
    }

    public List<Receipt> receipts() {
        return Collections.unmodifiableList(receipts);
    }

    public List<ParticipantWeighting> weightings() {
        return Collections.unmodifiableList(weightings);
    }
}
