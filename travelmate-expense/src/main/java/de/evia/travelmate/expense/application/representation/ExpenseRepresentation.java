package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.AdvancePayment;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.expense.PartySettlement;
import de.evia.travelmate.expense.domain.expense.Receipt;
import de.evia.travelmate.expense.domain.expense.SettlementPlan;
import de.evia.travelmate.expense.domain.trip.TripParticipant;

public record ExpenseRepresentation(
    UUID expenseId,
    UUID tripId,
    ExpenseStatus status,
    boolean reviewRequired,
    List<ReceiptRepresentation> receipts,
    List<WeightingRepresentation> weightings,
    Map<UUID, BigDecimal> balances,
    List<TransferRepresentation> transfers,
    List<CategoryBreakdownRepresentation> categoryBreakdown,
    List<ParticipantSummaryRepresentation> participantSummaries,
    List<DailyCostRepresentation> dailyCosts,
    BigDecimal totalAmount,
    List<PartySettlementRepresentation> partySettlements,
    List<PartyTransferRepresentation> partyTransfers,
    List<AdvancePaymentRepresentation> advancePayments
) {

    public static ExpenseRepresentation from(final Expense expense) {
        return from(expense, null, null, List.of());
    }

    public static ExpenseRepresentation from(final Expense expense,
                                              final LocalDate tripStartDate,
                                              final LocalDate tripEndDate) {
        return from(expense, tripStartDate, tripEndDate, List.of());
    }

    public static ExpenseRepresentation from(final Expense expense,
                                              final LocalDate tripStartDate,
                                              final LocalDate tripEndDate,
                                              final List<TripParticipant> participants) {
        final List<ReceiptRepresentation> receipts = expense.receipts().stream()
            .map(r -> new ReceiptRepresentation(
                r.receiptId().value(),
                r.description(),
                r.amount().value(),
                r.paidBy(),
                r.submittedBy(),
                r.date(),
                r.category(),
                r.reviewStatus(),
                r.reviewerId(),
                r.rejectionReason()
            ))
            .toList();

        final List<WeightingRepresentation> weightings = expense.weightings().stream()
            .map(w -> new WeightingRepresentation(w.participantId(), w.weight()))
            .toList();

        final BigDecimal totalAmount = expense.receipts().stream()
            .map(r -> r.amount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final SettlementPlan plan = expense.calculateSettlementPlan();
        final List<TransferRepresentation> transfers = plan.transfers().stream()
            .map(t -> new TransferRepresentation(t.from(), t.to(), t.amount()))
            .toList();

        final List<CategoryBreakdownRepresentation> categoryBreakdown =
            buildCategoryBreakdown(expense.receipts());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();
        final List<ParticipantSummaryRepresentation> participantSummaries =
            buildParticipantSummaries(expense.receipts(), expense.weightings(), balances, totalAmount);

        final List<DailyCostRepresentation> dailyCosts =
            buildDailyCosts(expense.receipts(), tripStartDate, tripEndDate);

        final List<PartySettlementRepresentation> partySettlements =
            buildPartySettlements(balances, expense.weightings(), expense.receipts(), participants);
        final List<PartyTransferRepresentation> partyTransfers =
            buildPartyTransfers(balances, participants);

        final List<AdvancePaymentRepresentation> advancePayments = expense.advancePayments().stream()
            .map(ap -> new AdvancePaymentRepresentation(
                ap.advancePaymentId().value(),
                ap.partyTenantId(),
                ap.partyName(),
                ap.amount(),
                ap.paid()
            ))
            .toList();

        return new ExpenseRepresentation(
            expense.expenseId().value(),
            expense.tripId(),
            expense.status(),
            expense.reviewRequired(),
            receipts,
            weightings,
            balances,
            transfers,
            categoryBreakdown,
            participantSummaries,
            dailyCosts,
            totalAmount,
            partySettlements,
            partyTransfers,
            advancePayments
        );
    }

    private static List<CategoryBreakdownRepresentation> buildCategoryBreakdown(
            final List<Receipt> receipts) {
        final Map<ExpenseCategory, BigDecimal> totals = new HashMap<>();
        final Map<ExpenseCategory, Integer> counts = new HashMap<>();

        for (final Receipt receipt : receipts) {
            final ExpenseCategory cat = receipt.category();
            totals.merge(cat, receipt.amount().value(), BigDecimal::add);
            counts.merge(cat, 1, Integer::sum);
        }

        return totals.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry<ExpenseCategory, BigDecimal>::getValue).reversed())
            .map(e -> new CategoryBreakdownRepresentation(e.getKey(), e.getValue(), counts.get(e.getKey())))
            .toList();
    }

    private static List<ParticipantSummaryRepresentation> buildParticipantSummaries(
            final List<Receipt> receipts,
            final List<ParticipantWeighting> weightings,
            final Map<UUID, BigDecimal> balances,
            final BigDecimal totalAmount) {
        final BigDecimal totalWeight = weightings.stream()
            .map(ParticipantWeighting::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Map<UUID, BigDecimal> paid = new HashMap<>();
        for (final Receipt receipt : receipts) {
            paid.merge(receipt.paidBy(), receipt.amount().value(), BigDecimal::add);
        }

        return weightings.stream()
            .map(w -> {
                final BigDecimal participantPaid = paid.getOrDefault(w.participantId(), BigDecimal.ZERO);
                final BigDecimal fairShare = totalWeight.signum() == 0
                    ? BigDecimal.ZERO
                    : totalAmount.multiply(w.weight())
                        .divide(totalWeight, 2, RoundingMode.HALF_UP);
                final BigDecimal balance = balances.getOrDefault(w.participantId(), BigDecimal.ZERO);
                return new ParticipantSummaryRepresentation(
                    w.participantId(), participantPaid, fairShare, balance);
            })
            .toList();
    }

    static List<DailyCostRepresentation> buildDailyCosts(
            final List<Receipt> receipts,
            final LocalDate tripStartDate,
            final LocalDate tripEndDate) {
        if (tripStartDate == null || tripEndDate == null) {
            return List.of();
        }

        final Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        final Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (LocalDate date = tripStartDate; !date.isAfter(tripEndDate); date = date.plusDays(1)) {
            totals.put(date, BigDecimal.ZERO);
            counts.put(date, 0);
        }

        for (final Receipt receipt : receipts) {
            final LocalDate date = receipt.date();
            if (date != null) {
                totals.merge(date, receipt.amount().value(), BigDecimal::add);
                counts.merge(date, 1, Integer::sum);
            }
        }

        final List<DailyCostRepresentation> result = new ArrayList<>();
        for (final Map.Entry<LocalDate, BigDecimal> entry : totals.entrySet()) {
            result.add(new DailyCostRepresentation(
                entry.getKey(), entry.getValue(), counts.get(entry.getKey())));
        }
        return result;
    }

    private static List<PartySettlementRepresentation> buildPartySettlements(
            final Map<UUID, BigDecimal> individualBalances,
            final List<ParticipantWeighting> weightings,
            final List<Receipt> receipts,
            final List<TripParticipant> participants) {
        final Map<UUID, UUID> participantToParty = buildParticipantToPartyMap(participants);
        if (participantToParty.isEmpty()) {
            return List.of();
        }

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, participantToParty);

        final Map<UUID, String> partyNames = new HashMap<>();
        for (final TripParticipant p : participants) {
            if (p.hasPartyInfo()) {
                partyNames.putIfAbsent(p.partyTenantId(), p.partyName());
            }
        }

        final BigDecimal totalWeight = weightings.stream()
            .map(ParticipantWeighting::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal totalAmount = receipts.stream()
            .map(r -> r.amount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Map<UUID, BigDecimal> partyPaid = new HashMap<>();
        for (final Receipt receipt : receipts) {
            final UUID partyId = participantToParty.get(receipt.paidBy());
            if (partyId != null) {
                partyPaid.merge(partyId, receipt.amount().value(), BigDecimal::add);
            }
        }

        final Map<UUID, BigDecimal> partyOwed = new HashMap<>();
        for (final ParticipantWeighting w : weightings) {
            final UUID partyId = participantToParty.get(w.participantId());
            if (partyId != null && totalWeight.signum() > 0) {
                final BigDecimal share = totalAmount.multiply(w.weight())
                    .divide(totalWeight, 2, RoundingMode.HALF_UP);
                partyOwed.merge(partyId, share, BigDecimal::add);
            }
        }

        final Map<UUID, List<String>> partyMembers = new HashMap<>();
        for (final TripParticipant p : participants) {
            if (p.hasPartyInfo()) {
                partyMembers.computeIfAbsent(p.partyTenantId(), k -> new ArrayList<>())
                    .add(p.name());
            }
        }

        return partyBalances.entrySet().stream()
            .map(e -> new PartySettlementRepresentation(
                e.getKey(),
                partyNames.getOrDefault(e.getKey(), e.getKey().toString()),
                partyPaid.getOrDefault(e.getKey(), BigDecimal.ZERO),
                partyOwed.getOrDefault(e.getKey(), BigDecimal.ZERO),
                e.getValue(),
                partyMembers.getOrDefault(e.getKey(), List.of())
            ))
            .toList();
    }

    private static List<PartyTransferRepresentation> buildPartyTransfers(
            final Map<UUID, BigDecimal> individualBalances,
            final List<TripParticipant> participants) {
        final Map<UUID, UUID> participantToParty = buildParticipantToPartyMap(participants);
        if (participantToParty.isEmpty()) {
            return List.of();
        }

        final Map<UUID, String> partyNames = new HashMap<>();
        for (final TripParticipant p : participants) {
            if (p.hasPartyInfo()) {
                partyNames.putIfAbsent(p.partyTenantId(), p.partyName());
            }
        }

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, participantToParty);
        final List<PartySettlement.PartyTransfer> transfers =
            PartySettlement.calculateTransfers(partyBalances);

        return transfers.stream()
            .map(t -> new PartyTransferRepresentation(
                t.fromPartyId(),
                partyNames.getOrDefault(t.fromPartyId(), t.fromPartyId().toString()),
                t.toPartyId(),
                partyNames.getOrDefault(t.toPartyId(), t.toPartyId().toString()),
                t.amount()
            ))
            .toList();
    }

    private static Map<UUID, UUID> buildParticipantToPartyMap(final List<TripParticipant> participants) {
        final Map<UUID, UUID> map = new HashMap<>();
        for (final TripParticipant p : participants) {
            if (p.hasPartyInfo()) {
                map.put(p.participantId(), p.partyTenantId());
            }
        }
        return map;
    }
}
