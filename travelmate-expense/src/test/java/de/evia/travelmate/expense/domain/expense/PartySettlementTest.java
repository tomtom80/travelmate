package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PartySettlementTest {

    private static final UUID PARTY_SCHMIDT = UUID.randomUUID();
    private static final UUID PARTY_MUELLER = UUID.randomUUID();
    private static final UUID PARTY_WEBER = UUID.randomUUID();

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CHILD_ALICE = UUID.randomUUID();
    private static final UUID CHARLIE = UUID.randomUUID();
    private static final UUID DAVE = UUID.randomUUID();

    // --- aggregateByParty ---

    @Test
    void aggregateByPartyGroupsBalancesByPartyId() {
        final Map<UUID, BigDecimal> individualBalances = new HashMap<>();
        individualBalances.put(ALICE, new BigDecimal("30.00"));
        individualBalances.put(BOB, new BigDecimal("20.00"));
        individualBalances.put(CHARLIE, new BigDecimal("-50.00"));

        final Map<UUID, UUID> participantToParty = Map.of(
            ALICE, PARTY_SCHMIDT,
            BOB, PARTY_SCHMIDT,
            CHARLIE, PARTY_MUELLER
        );

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, participantToParty);

        assertThat(partyBalances).hasSize(2);
        assertThat(partyBalances.get(PARTY_SCHMIDT)).isEqualByComparingTo("50.00");
        assertThat(partyBalances.get(PARTY_MUELLER)).isEqualByComparingTo("-50.00");
    }

    @Test
    void aggregateByPartyIgnoresParticipantsWithoutPartyId() {
        final Map<UUID, BigDecimal> individualBalances = new HashMap<>();
        individualBalances.put(ALICE, new BigDecimal("30.00"));
        individualBalances.put(BOB, new BigDecimal("-30.00"));

        final Map<UUID, UUID> participantToParty = Map.of(
            ALICE, PARTY_SCHMIDT
            // BOB has no party mapping
        );

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, participantToParty);

        assertThat(partyBalances).hasSize(1);
        assertThat(partyBalances.get(PARTY_SCHMIDT)).isEqualByComparingTo("30.00");
    }

    @Test
    void aggregateByPartyEmptyWhenNoMappings() {
        final Map<UUID, BigDecimal> individualBalances = Map.of(
            ALICE, new BigDecimal("50.00"),
            BOB, new BigDecimal("-50.00")
        );

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, Map.of());

        assertThat(partyBalances).isEmpty();
    }

    @Test
    void aggregateByPartyHandlesThreePartiesWithChildren() {
        final Map<UUID, BigDecimal> individualBalances = new HashMap<>();
        individualBalances.put(ALICE, new BigDecimal("80.00"));
        individualBalances.put(CHILD_ALICE, new BigDecimal("0.00"));
        individualBalances.put(CHARLIE, new BigDecimal("-40.00"));
        individualBalances.put(DAVE, new BigDecimal("-40.00"));

        final Map<UUID, UUID> participantToParty = Map.of(
            ALICE, PARTY_SCHMIDT,
            CHILD_ALICE, PARTY_SCHMIDT,
            CHARLIE, PARTY_MUELLER,
            DAVE, PARTY_WEBER
        );

        final Map<UUID, BigDecimal> partyBalances = PartySettlement.aggregateByParty(
            individualBalances, participantToParty);

        assertThat(partyBalances).hasSize(3);
        assertThat(partyBalances.get(PARTY_SCHMIDT)).isEqualByComparingTo("80.00");
        assertThat(partyBalances.get(PARTY_MUELLER)).isEqualByComparingTo("-40.00");
        assertThat(partyBalances.get(PARTY_WEBER)).isEqualByComparingTo("-40.00");
    }

    // --- calculateTransfers ---

    @Test
    void calculateTransfersEmptyWhenAllZero() {
        final Map<UUID, BigDecimal> partyBalances = Map.of(
            PARTY_SCHMIDT, BigDecimal.ZERO,
            PARTY_MUELLER, BigDecimal.ZERO
        );

        final List<PartySettlement.PartyTransfer> transfers =
            PartySettlement.calculateTransfers(partyBalances);

        assertThat(transfers).isEmpty();
    }

    @Test
    void calculateTransfersTwoPartiesOneTransfer() {
        final Map<UUID, BigDecimal> partyBalances = Map.of(
            PARTY_SCHMIDT, new BigDecimal("50.00"),
            PARTY_MUELLER, new BigDecimal("-50.00")
        );

        final List<PartySettlement.PartyTransfer> transfers =
            PartySettlement.calculateTransfers(partyBalances);

        assertThat(transfers).hasSize(1);
        final PartySettlement.PartyTransfer transfer = transfers.getFirst();
        assertThat(transfer.fromPartyId()).isEqualTo(PARTY_MUELLER);
        assertThat(transfer.toPartyId()).isEqualTo(PARTY_SCHMIDT);
        assertThat(transfer.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateTransfersThreePartiesTwoTransfers() {
        final Map<UUID, BigDecimal> partyBalances = new HashMap<>();
        partyBalances.put(PARTY_SCHMIDT, new BigDecimal("100.00"));
        partyBalances.put(PARTY_MUELLER, new BigDecimal("-60.00"));
        partyBalances.put(PARTY_WEBER, new BigDecimal("-40.00"));

        final List<PartySettlement.PartyTransfer> transfers =
            PartySettlement.calculateTransfers(partyBalances);

        assertThat(transfers).hasSize(2);
        final BigDecimal totalTransferred = transfers.stream()
            .map(PartySettlement.PartyTransfer::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("100.00");

        assertThat(transfers).allSatisfy(t ->
            assertThat(t.amount().signum()).isGreaterThan(0)
        );
    }

    @Test
    void calculateTransfersEmptyWhenNoBalances() {
        final List<PartySettlement.PartyTransfer> transfers =
            PartySettlement.calculateTransfers(Map.of());

        assertThat(transfers).isEmpty();
    }
}
