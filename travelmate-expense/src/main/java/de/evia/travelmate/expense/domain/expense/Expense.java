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
    private final boolean reviewRequired;
    private final List<Receipt> receipts;
    private final List<ParticipantWeighting> weightings;
    private ExpenseStatus status;

    public Expense(final ExpenseId expenseId,
                   final TenantId tenantId,
                   final UUID tripId,
                   final ExpenseStatus status,
                   final List<Receipt> receipts,
                   final List<ParticipantWeighting> weightings,
                   final boolean reviewRequired) {
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
        this.reviewRequired = reviewRequired;
    }

    public static Expense create(final TenantId tenantId,
                                 final UUID tripId,
                                 final List<ParticipantWeighting> weightings) {
        return create(tenantId, tripId, weightings, false);
    }

    public static Expense create(final TenantId tenantId,
                                 final UUID tripId,
                                 final List<ParticipantWeighting> weightings,
                                 final boolean reviewRequired) {
        argumentIsTrue(!weightings.isEmpty(), "At least one participant weighting is required.");
        final Expense expense = new Expense(
            new ExpenseId(UUID.randomUUID()),
            tenantId,
            tripId,
            ExpenseStatus.OPEN,
            List.of(),
            weightings,
            reviewRequired
        );
        expense.registerEvent(new ExpenseCreated(
            tenantId.value(), tripId, expense.expenseId.value(), LocalDate.now()
        ));
        return expense;
    }

    public void addReceipt(final String description, final Amount amount,
                           final UUID paidBy, final UUID submittedBy,
                           final LocalDate date, final ExpenseCategory category) {
        assertNotSettled();
        argumentIsTrue(hasParticipant(paidBy),
            "Payer " + paidBy + " is not a participant in this expense.");
        final ReviewStatus initialStatus = reviewRequired
            ? ReviewStatus.SUBMITTED
            : ReviewStatus.APPROVED;
        final Receipt receipt = new Receipt(
            new ReceiptId(UUID.randomUUID()), description, amount, paidBy, submittedBy,
            date, category, initialStatus, null, null
        );
        receipts.add(receipt);
        evaluateSettlementReadiness();
    }

    public void approveReceipt(final ReceiptId receiptId, final UUID reviewerId) {
        assertNotSettled();
        findReceipt(receiptId).approve(reviewerId);
        evaluateSettlementReadiness();
    }

    public void rejectReceipt(final ReceiptId receiptId, final UUID reviewerId,
                              final String reason) {
        assertNotSettled();
        findReceipt(receiptId).reject(reviewerId, reason);
        evaluateSettlementReadiness();
    }

    public void resubmitReceipt(final ReceiptId receiptId, final String description,
                                final Amount amount, final LocalDate date,
                                final ExpenseCategory category) {
        assertNotSettled();
        findReceipt(receiptId).resubmit(description, amount, date, category);
        evaluateSettlementReadiness();
    }

    public void removeReceipt(final ReceiptId receiptId) {
        assertNotSettled();
        final boolean removed = receipts.removeIf(
            r -> r.receiptId().equals(receiptId));
        argumentIsTrue(removed, "Receipt " + receiptId.value() + " not found.");
        evaluateSettlementReadiness();
    }

    public void updateWeighting(final UUID participantId, final BigDecimal newWeight) {
        assertNotSettled();
        argumentIsNotNull(newWeight, "weight");
        argumentIsTrue(newWeight.compareTo(BigDecimal.ZERO) >= 0,
            "Weight must not be negative.");
        weightings.removeIf(w -> w.participantId().equals(participantId));
        weightings.add(new ParticipantWeighting(participantId, newWeight));
    }

    public void settle() {
        argumentIsTrue(
            status == ExpenseStatus.OPEN || status == ExpenseStatus.READY_FOR_SETTLEMENT,
            "Expense must be OPEN or READY_FOR_SETTLEMENT to settle.");
        argumentIsTrue(!receipts.isEmpty(), "Cannot settle an expense with no receipts.");
        argumentIsTrue(allReceiptsApproved(), "All receipts must be approved before settling.");
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

    public SettlementPlan calculateSettlementPlan() {
        return SettlementPlan.from(calculateBalances());
    }

    private boolean allReceiptsApproved() {
        return receipts.stream()
            .allMatch(r -> r.reviewStatus() == ReviewStatus.APPROVED);
    }

    private void evaluateSettlementReadiness() {
        if (!receipts.isEmpty() && allReceiptsApproved()) {
            this.status = ExpenseStatus.READY_FOR_SETTLEMENT;
        } else {
            this.status = ExpenseStatus.OPEN;
        }
    }

    private Receipt findReceipt(final ReceiptId receiptId) {
        return receipts.stream()
            .filter(r -> r.receiptId().equals(receiptId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Receipt " + receiptId.value() + " not found."));
    }

    private boolean hasParticipant(final UUID participantId) {
        return weightings.stream()
            .anyMatch(w -> w.participantId().equals(participantId));
    }

    private void assertNotSettled() {
        if (status == ExpenseStatus.SETTLED) {
            throw new IllegalStateException("Expense is SETTLED, modifications not allowed.");
        }
    }

    public ExpenseId expenseId() { return expenseId; }
    public TenantId tenantId() { return tenantId; }
    public UUID tripId() { return tripId; }
    public ExpenseStatus status() { return status; }
    public boolean reviewRequired() { return reviewRequired; }
    public List<Receipt> receipts() { return Collections.unmodifiableList(receipts); }
    public List<ParticipantWeighting> weightings() { return Collections.unmodifiableList(weightings); }
}
