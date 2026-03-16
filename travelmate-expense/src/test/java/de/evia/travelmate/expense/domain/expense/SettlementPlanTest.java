package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SettlementPlanTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CHARLIE = UUID.randomUUID();

    @Test
    void emptyBalancesProduceEmptyPlan() {
        final SettlementPlan plan = SettlementPlan.from(Map.of());

        assertThat(plan.transfers()).isEmpty();
    }

    @Test
    void allZeroBalancesProduceEmptyPlan() {
        final Map<UUID, BigDecimal> balances = Map.of(
            ALICE, BigDecimal.ZERO,
            BOB, BigDecimal.ZERO
        );

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers()).isEmpty();
    }

    @Test
    void twoParticipantsOneTransfer() {
        final Map<UUID, BigDecimal> balances = Map.of(
            ALICE, new BigDecimal("50.00"),
            BOB, new BigDecimal("-50.00")
        );

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers()).hasSize(1);
        final Transfer transfer = plan.transfers().getFirst();
        assertThat(transfer.from()).isEqualTo(BOB);
        assertThat(transfer.to()).isEqualTo(ALICE);
        assertThat(transfer.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void threeParticipantsTwoTransfers() {
        final Map<UUID, BigDecimal> balances = new HashMap<>();
        balances.put(ALICE, new BigDecimal("70.00"));
        balances.put(BOB, new BigDecimal("-30.00"));
        balances.put(CHARLIE, new BigDecimal("-40.00"));

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers()).hasSize(2);
        final BigDecimal totalTransferred = plan.transfers().stream()
            .map(Transfer::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("70.00");

        // All transfers should have positive amounts
        assertThat(plan.transfers()).allSatisfy(t ->
            assertThat(t.amount().signum()).isGreaterThan(0)
        );
    }

    @Test
    void threeParticipantsOnePaidAll() {
        final Map<UUID, BigDecimal> balances = new HashMap<>();
        balances.put(ALICE, new BigDecimal("100.00"));
        balances.put(BOB, new BigDecimal("-60.00"));
        balances.put(CHARLIE, new BigDecimal("-40.00"));

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers()).hasSize(2);
        // Both BOB and CHARLIE pay ALICE
        assertThat(plan.transfers()).allSatisfy(t ->
            assertThat(t.to()).isEqualTo(ALICE)
        );
    }

    @Test
    void transfersFromAreDebtors() {
        final Map<UUID, BigDecimal> balances = Map.of(
            ALICE, new BigDecimal("50.00"),
            BOB, new BigDecimal("-50.00")
        );

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers().getFirst().from()).isEqualTo(BOB);
        assertThat(plan.transfers().getFirst().to()).isEqualTo(ALICE);
    }

    @Test
    void roundingDoesNotProduceZeroTransfers() {
        final Map<UUID, BigDecimal> balances = new HashMap<>();
        balances.put(ALICE, new BigDecimal("33.33"));
        balances.put(BOB, new BigDecimal("-16.67"));
        balances.put(CHARLIE, new BigDecimal("-16.66"));

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.transfers()).allSatisfy(t ->
            assertThat(t.amount().signum()).isGreaterThan(0)
        );
    }

    @Test
    void totalAmountReturnsSum() {
        final Map<UUID, BigDecimal> balances = Map.of(
            ALICE, new BigDecimal("50.00"),
            BOB, new BigDecimal("-50.00")
        );

        final SettlementPlan plan = SettlementPlan.from(balances);

        assertThat(plan.totalAmount()).isEqualByComparingTo("50.00");
    }
}
