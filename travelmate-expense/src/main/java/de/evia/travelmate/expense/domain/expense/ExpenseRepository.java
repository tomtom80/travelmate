package de.evia.travelmate.expense.domain.expense;

import java.util.Optional;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(ExpenseId expenseId);

    Optional<Expense> findByTripId(TenantId tenantId, UUID tripId);

    boolean existsByTripId(UUID tripId);
}
