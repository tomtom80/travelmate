package de.evia.travelmate.expense.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.expense.AdvancePayment;
import de.evia.travelmate.expense.domain.expense.AdvancePaymentId;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseId;
import de.evia.travelmate.expense.domain.expense.ExpenseRepository;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.expense.Receipt;
import de.evia.travelmate.expense.domain.expense.ReceiptId;

@Repository
@Transactional
public class ExpenseRepositoryAdapter implements ExpenseRepository {

    private final ExpenseJpaRepository jpaRepository;

    public ExpenseRepositoryAdapter(final ExpenseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Expense save(final Expense expense) {
        final ExpenseJpaEntity entity = jpaRepository.findById(expense.expenseId().value())
            .orElseGet(() -> new ExpenseJpaEntity(
                expense.expenseId().value(),
                expense.tenantId().value(),
                expense.tripId(),
                expense.status(),
                expense.reviewRequired()
            ));
        entity.setStatus(expense.status());
        syncReceipts(entity, expense);
        syncWeightings(entity, expense);
        syncAdvancePayments(entity, expense);
        jpaRepository.save(entity);
        return expense;
    }

    @Override
    public Optional<Expense> findById(final ExpenseId expenseId) {
        return jpaRepository.findById(expenseId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Expense> findByTripId(final TenantId tenantId, final UUID tripId) {
        return jpaRepository.findByTenantIdAndTripId(tenantId.value(), tripId)
            .map(this::toDomain);
    }

    @Override
    public boolean existsByTripId(final UUID tripId) {
        return jpaRepository.existsByTripId(tripId);
    }

    private void syncReceipts(final ExpenseJpaEntity entity, final Expense expense) {
        entity.getReceipts().removeIf(r ->
            expense.receipts().stream()
                .noneMatch(dr -> dr.receiptId().value().equals(r.getReceiptId())));

        for (final Receipt receipt : expense.receipts()) {
            final var existing = entity.getReceipts().stream()
                .filter(r -> r.getReceiptId().equals(receipt.receiptId().value()))
                .findFirst();
            if (existing.isPresent()) {
                final ReceiptJpaEntity r = existing.get();
                r.setDescription(receipt.description());
                r.setAmount(receipt.amount().value());
                r.setDate(receipt.date());
                r.setCategory(receipt.category());
                r.setReviewStatus(receipt.reviewStatus());
                r.setReviewerId(receipt.reviewerId());
                r.setRejectionReason(receipt.rejectionReason());
            } else {
                entity.getReceipts().add(new ReceiptJpaEntity(
                    receipt.receiptId().value(),
                    entity,
                    receipt.description(),
                    receipt.amount().value(),
                    receipt.paidBy(),
                    receipt.submittedBy(),
                    receipt.date(),
                    receipt.category(),
                    receipt.reviewStatus(),
                    receipt.reviewerId(),
                    receipt.rejectionReason()
                ));
            }
        }
    }

    private void syncWeightings(final ExpenseJpaEntity entity, final Expense expense) {
        entity.getWeightings().clear();
        for (final ParticipantWeighting weighting : expense.weightings()) {
            entity.getWeightings().add(new WeightingJpaEntity(
                entity,
                weighting.participantId(),
                weighting.weight()
            ));
        }
    }

    private void syncAdvancePayments(final ExpenseJpaEntity entity, final Expense expense) {
        entity.getAdvancePayments().removeIf(ap ->
            expense.advancePayments().stream()
                .noneMatch(dap -> dap.advancePaymentId().value().equals(ap.getAdvancePaymentId())));

        for (final AdvancePayment ap : expense.advancePayments()) {
            final var existing = entity.getAdvancePayments().stream()
                .filter(e -> e.getAdvancePaymentId().equals(ap.advancePaymentId().value()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setPaid(ap.paid());
                existing.get().setAmount(ap.amount());
                existing.get().setPartyName(ap.partyName());
            } else {
                entity.getAdvancePayments().add(new AdvancePaymentJpaEntity(
                    ap.advancePaymentId().value(),
                    entity,
                    ap.partyTenantId(),
                    ap.partyName(),
                    ap.amount(),
                    ap.paid()
                ));
            }
        }
    }

    private Expense toDomain(final ExpenseJpaEntity entity) {
        final var receipts = entity.getReceipts().stream()
            .map(r -> new Receipt(
                new ReceiptId(r.getReceiptId()),
                r.getDescription(),
                new Amount(r.getAmount()),
                r.getPaidBy(),
                r.getSubmittedBy(),
                r.getDate(),
                r.getCategory(),
                r.getReviewStatus(),
                r.getReviewerId(),
                r.getRejectionReason()
            ))
            .toList();

        final var weightings = entity.getWeightings().stream()
            .map(w -> new ParticipantWeighting(w.getParticipantId(), w.getWeight()))
            .toList();

        final var advancePayments = entity.getAdvancePayments().stream()
            .map(ap -> new AdvancePayment(
                new AdvancePaymentId(ap.getAdvancePaymentId()),
                ap.getPartyTenantId(),
                ap.getPartyName(),
                ap.getAmount(),
                ap.isPaid()
            ))
            .toList();

        return new Expense(
            new ExpenseId(entity.getExpenseId()),
            new TenantId(entity.getTenantId()),
            entity.getTripId(),
            entity.getStatus(),
            receipts,
            weightings,
            advancePayments,
            entity.isReviewRequired()
        );
    }
}
