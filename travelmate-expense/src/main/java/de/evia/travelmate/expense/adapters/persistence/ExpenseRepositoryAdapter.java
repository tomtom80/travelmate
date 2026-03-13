package de.evia.travelmate.expense.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.TenantId;
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
                expense.status()
            ));
        entity.setStatus(expense.status());
        syncReceipts(entity, expense);
        syncWeightings(entity, expense);
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
            final boolean exists = entity.getReceipts().stream()
                .anyMatch(r -> r.getReceiptId().equals(receipt.receiptId().value()));
            if (!exists) {
                entity.getReceipts().add(new ReceiptJpaEntity(
                    receipt.receiptId().value(),
                    entity,
                    receipt.description(),
                    receipt.amount().value(),
                    receipt.paidBy(),
                    receipt.date()
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

    private Expense toDomain(final ExpenseJpaEntity entity) {
        final var receipts = entity.getReceipts().stream()
            .map(r -> new Receipt(
                new ReceiptId(r.getReceiptId()),
                r.getDescription(),
                new Amount(r.getAmount()),
                r.getPaidBy(),
                r.getDate()
            ))
            .toList();

        final var weightings = entity.getWeightings().stream()
            .map(w -> new ParticipantWeighting(w.getParticipantId(), w.getWeight()))
            .toList();

        return new Expense(
            new ExpenseId(entity.getExpenseId()),
            new TenantId(entity.getTenantId()),
            entity.getTripId(),
            entity.getStatus(),
            receipts,
            weightings
        );
    }
}
