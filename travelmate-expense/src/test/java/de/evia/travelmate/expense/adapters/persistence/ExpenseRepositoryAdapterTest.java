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
import de.evia.travelmate.expense.domain.expense.AdvancePayment;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseRepository;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.expense.ReceiptId;
import de.evia.travelmate.expense.domain.expense.ReviewStatus;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseRepositoryAdapterTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void savesAndFindsByTripId() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );

        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findByTripId(tenantId, tripId);
        assertThat(found).isPresent();
        assertThat(found.get().expenseId()).isEqualTo(expense.expenseId());
        assertThat(found.get().tenantId()).isEqualTo(tenantId);
        assertThat(found.get().tripId()).isEqualTo(tripId);
        assertThat(found.get().status()).isEqualTo(ExpenseStatus.OPEN);
    }

    @Test
    void savesAndFindsById() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );

        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().expenseId()).isEqualTo(expense.expenseId());
    }

    @Test
    void persistsReceipts() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expense.addReceipt("Groceries", new Amount(new BigDecimal("42.50")),
            participantId, participantId, LocalDate.of(2025, 7, 15), null);
        expense.addReceipt("Gas", new Amount(new BigDecimal("65.00")),
            participantId, participantId, LocalDate.of(2025, 7, 16), null);

        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().receipts()).hasSize(2);
        assertThat(found.get().receipts())
            .extracting(r -> r.description())
            .containsExactlyInAnyOrder("Groceries", "Gas");
        assertThat(found.get().receipts())
            .extracting(r -> r.paidBy())
            .containsOnly(participantId);
    }

    @Test
    void persistsWeightings() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participant1 = UUID.randomUUID();
        final UUID participant2 = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(
                new ParticipantWeighting(participant1, new BigDecimal("1.0")),
                new ParticipantWeighting(participant2, new BigDecimal("0.5"))
            )
        );

        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().weightings()).hasSize(2);
        assertThat(found.get().weightings())
            .extracting(ParticipantWeighting::participantId)
            .containsExactlyInAnyOrder(participant1, participant2);
    }

    @Test
    void existsByTripIdReturnsTrue() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(UUID.randomUUID(), BigDecimal.ONE))
        );

        expenseRepository.save(expense);

        assertThat(expenseRepository.existsByTripId(tripId)).isTrue();
    }

    @Test
    void existsByTripIdReturnsFalse() {
        assertThat(expenseRepository.existsByTripId(UUID.randomUUID())).isFalse();
    }

    @Test
    void persistsReviewFields() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID alice = UUID.randomUUID();
        final UUID bob = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(
                new ParticipantWeighting(alice, BigDecimal.ONE),
                new ParticipantWeighting(bob, BigDecimal.ONE)
            ),
            true
        );
        expense.addReceipt("Groceries", new Amount(new BigDecimal("42.50")),
            alice, alice, LocalDate.of(2025, 7, 15), null);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        expenseRepository.save(expense);

        // Verify initial SUBMITTED status persisted
        final Expense afterAdd = expenseRepository.findByTripId(tenantId, tripId).orElseThrow();
        assertThat(afterAdd.reviewRequired()).isTrue();
        assertThat(afterAdd.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.SUBMITTED);
        assertThat(afterAdd.receipts().getFirst().submittedBy()).isEqualTo(alice);

        // Approve and verify round-trip
        afterAdd.approveReceipt(receiptId, bob);
        expenseRepository.save(afterAdd);

        final Expense afterApprove = expenseRepository.findByTripId(tenantId, tripId).orElseThrow();
        assertThat(afterApprove.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(afterApprove.receipts().getFirst().reviewerId()).isEqualTo(bob);
        assertThat(afterApprove.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);
    }

    @Test
    void persistsRejectionFields() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID alice = UUID.randomUUID();
        final UUID bob = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(
                new ParticipantWeighting(alice, BigDecimal.ONE),
                new ParticipantWeighting(bob, BigDecimal.ONE)
            ),
            true
        );
        expense.addReceipt("Groceries", new Amount(new BigDecimal("42.50")),
            alice, alice, LocalDate.of(2025, 7, 15), null);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        expense.rejectReceipt(receiptId, bob, "Wrong amount");
        expenseRepository.save(expense);

        final Expense found = expenseRepository.findByTripId(tenantId, tripId).orElseThrow();
        assertThat(found.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(found.receipts().getFirst().reviewerId()).isEqualTo(bob);
        assertThat(found.receipts().getFirst().rejectionReason()).isEqualTo("Wrong amount");
    }

    @Test
    void persistsAdvancePayments() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        final UUID partyA = UUID.randomUUID();
        final UUID partyB = UUID.randomUUID();
        expense.confirmAdvancePayments(new BigDecimal("500.00"), List.of(
            new Expense.PartyInfo(partyA, "Familie Mueller"),
            new Expense.PartyInfo(partyB, "Familie Schmidt")
        ));

        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().advancePayments()).hasSize(2);
        assertThat(found.get().advancePayments()).allSatisfy(
            ap -> assertThat(ap.amount()).isEqualByComparingTo("500.00"));
        assertThat(found.get().advancePayments()).extracting(AdvancePayment::partyTenantId)
            .containsExactlyInAnyOrder(partyA, partyB);
    }

    @Test
    void persistsAdvancePaymentPaidToggle() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expense.confirmAdvancePayments(new BigDecimal("500.00"), List.of(
            new Expense.PartyInfo(UUID.randomUUID(), "Familie Mueller")
        ));
        expenseRepository.save(expense);

        final Expense loaded = expenseRepository.findById(expense.expenseId()).orElseThrow();
        loaded.toggleAdvancePaymentPaid(loaded.advancePayments().getFirst().advancePaymentId());
        expenseRepository.save(loaded);

        final Expense reloaded = expenseRepository.findById(expense.expenseId()).orElseThrow();
        assertThat(reloaded.advancePayments().getFirst().paid()).isTrue();
    }

    @Test
    void persistsAdvancePaymentRemoval() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expense.confirmAdvancePayments(new BigDecimal("500.00"), List.of(
            new Expense.PartyInfo(UUID.randomUUID(), "Familie Mueller")
        ));
        expenseRepository.save(expense);

        final Expense loaded = expenseRepository.findById(expense.expenseId()).orElseThrow();
        loaded.removeAdvancePayments();
        expenseRepository.save(loaded);

        final Expense reloaded = expenseRepository.findById(expense.expenseId()).orElseThrow();
        assertThat(reloaded.advancePayments()).isEmpty();
    }

    @Test
    void updatesStatusOnSave() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final UUID tripId = UUID.randomUUID();
        final UUID participantId = UUID.randomUUID();
        final Expense expense = Expense.create(
            tenantId, tripId,
            List.of(new ParticipantWeighting(participantId, BigDecimal.ONE))
        );
        expense.addReceipt("Dinner", new Amount(new BigDecimal("80.00")),
            participantId, participantId, LocalDate.of(2025, 7, 15), null);

        expenseRepository.save(expense);
        expense.settle();
        expenseRepository.save(expense);

        final Optional<Expense> found = expenseRepository.findById(expense.expenseId());
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(ExpenseStatus.SETTLED);
    }
}
