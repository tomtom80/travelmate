package de.evia.travelmate.expense.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseRepository;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    private static final TenantId TENANT_A = new TenantId(UUID.randomUUID());
    private static final TenantId TENANT_B = new TenantId(UUID.randomUUID());

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TripProjectionRepository tripProjectionRepository;

    @Test
    void expenseFromTenantANotVisibleToTenantB() {
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            TENANT_A, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expense.addReceipt("Groceries", new Amount(new BigDecimal("50.00")),
            participantId, participantId, LocalDate.of(2026, 7, 1), null);
        expenseRepository.save(expense);

        final Optional<Expense> foundByTenantA = expenseRepository.findByTripId(TENANT_A, tripId);
        final Optional<Expense> foundByTenantB = expenseRepository.findByTripId(TENANT_B, tripId);

        assertThat(foundByTenantA).isPresent();
        assertThat(foundByTenantB).isEmpty();
    }

    @Test
    void twoTenantsCanHaveExpensesForDifferentTrips() {
        final UUID tripA = UUID.randomUUID();
        final UUID tripB = UUID.randomUUID();
        final UUID participantA = UUID.randomUUID();
        final UUID participantB = UUID.randomUUID();

        expenseRepository.save(Expense.create(
            TENANT_A, tripA,
            List.of(new ParticipantWeighting(participantA, BigDecimal.ONE))
        ));
        expenseRepository.save(Expense.create(
            TENANT_B, tripB,
            List.of(new ParticipantWeighting(participantB, BigDecimal.ONE))
        ));

        assertThat(expenseRepository.findByTripId(TENANT_A, tripA)).isPresent();
        assertThat(expenseRepository.findByTripId(TENANT_A, tripB)).isEmpty();
        assertThat(expenseRepository.findByTripId(TENANT_B, tripB)).isPresent();
        assertThat(expenseRepository.findByTripId(TENANT_B, tripA)).isEmpty();
    }

    @Test
    void tripProjectionIsolatedByTenantId() {
        final UUID tripIdA = UUID.randomUUID();
        final UUID tripIdB = UUID.randomUUID();

        final TripProjection projectionA = TripProjection.create(tripIdA, TENANT_A, "Vacation A");
        projectionA.addParticipant(new TripParticipant(UUID.randomUUID(), "Alice"));
        tripProjectionRepository.save(projectionA);

        final TripProjection projectionB = TripProjection.create(tripIdB, TENANT_B, "Vacation B");
        projectionB.addParticipant(new TripParticipant(UUID.randomUUID(), "Bob"));
        tripProjectionRepository.save(projectionB);

        final Optional<TripProjection> foundA = tripProjectionRepository.findByTripId(tripIdA);
        final Optional<TripProjection> foundB = tripProjectionRepository.findByTripId(tripIdB);

        assertThat(foundA).isPresent();
        assertThat(foundA.get().tenantId()).isEqualTo(TENANT_A);
        assertThat(foundA.get().participants()).hasSize(1);
        assertThat(foundA.get().participants().getFirst().name()).isEqualTo("Alice");

        assertThat(foundB).isPresent();
        assertThat(foundB.get().tenantId()).isEqualTo(TENANT_B);
        assertThat(foundB.get().participants()).hasSize(1);
        assertThat(foundB.get().participants().getFirst().name()).isEqualTo("Bob");
    }

    @Test
    void findByIdDoesNotBypassTenantScopeOnExpenseAccess() {
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            TENANT_A, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().tenantId()).isEqualTo(TENANT_A);
    }
}
