package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.expense.domain.trip.TripParticipant;

class PartyAccountingTest {

    @Test
    void accommodationShareUsesWeightAndStayPeriodAcrossParties() {
        final UUID partyA = UUID.randomUUID();
        final UUID partyB = UUID.randomUUID();
        final UUID alice = UUID.randomUUID();
        final UUID bob = UUID.randomUUID();
        final UUID charlie = UUID.randomUUID();

        final List<PartyAccount> accounts = PartyAccounting.calculate(
            List.of(
                new ParticipantWeighting(alice, new BigDecimal("1.0")),
                new ParticipantWeighting(bob, new BigDecimal("1.0")),
                new ParticipantWeighting(charlie, new BigDecimal("0.5"))
            ),
            List.of(),
            List.of(),
            List.of(
                new TripParticipant(alice, "Alice", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), partyA, "Familie A"),
                new TripParticipant(bob, "Bob", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), partyA, "Familie A"),
                new TripParticipant(charlie, "Charlie", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), partyB, "Familie B")
            ),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
            new BigDecimal("300.00")
        );

        assertThat(accounts).hasSize(2);

        final PartyAccount familyA = accounts.stream()
            .filter(account -> account.partyTenantId().equals(partyA))
            .findFirst()
            .orElseThrow();
        assertThat(familyA.fairShare()).isEqualByComparingTo("225.00");
        assertThat(familyA.outstandingAmount()).isEqualByComparingTo("225.00");
        assertThat(familyA.entries())
            .extracting(PartyAccountEntry::type, PartyAccountEntry::label, PartyAccountEntry::amount)
            .containsExactly(
                tuple(PartyAccountEntryType.ACCOMMODATION_SHARE, "Alice - 4 Naechte x 1", new BigDecimal("-150.00")),
                tuple(PartyAccountEntryType.ACCOMMODATION_SHARE, "Bob - 2 Naechte x 1", new BigDecimal("-75.00"))
            );

        final PartyAccount familyB = accounts.stream()
            .filter(account -> account.partyTenantId().equals(partyB))
            .findFirst()
            .orElseThrow();
        assertThat(familyB.fairShare()).isEqualByComparingTo("75.00");
        assertThat(familyB.outstandingAmount()).isEqualByComparingTo("75.00");
        assertThat(familyB.entries())
            .extracting(PartyAccountEntry::type, PartyAccountEntry::label, PartyAccountEntry::amount)
            .containsExactly(tuple(PartyAccountEntryType.ACCOMMODATION_SHARE, "Charlie - 4 Naechte x 0.5", new BigDecimal("-75.00")));
    }

    @Test
    void receiptCreditsAppearAsIndividualTimelineEntriesForThePayingParty() {
        final UUID partyA = UUID.randomUUID();
        final UUID partyB = UUID.randomUUID();
        final UUID alice = UUID.randomUUID();
        final UUID bob = UUID.randomUUID();

        final List<PartyAccount> accounts = PartyAccounting.calculate(
            List.of(
                new ParticipantWeighting(alice, BigDecimal.ONE),
                new ParticipantWeighting(bob, BigDecimal.ONE)
            ),
            List.of(
                new Receipt(new ReceiptId(UUID.randomUUID()), "Supermarkt", new Amount(new BigDecimal("12.50")),
                    alice, alice, LocalDate.of(2026, 7, 2), ExpenseCategory.GROCERIES, ReviewStatus.APPROVED, null, null),
                new Receipt(new ReceiptId(UUID.randomUUID()), "Baeckerei", new Amount(new BigDecimal("7.40")),
                    alice, alice, LocalDate.of(2026, 7, 3), ExpenseCategory.GROCERIES, ReviewStatus.APPROVED, null, null)
            ),
            List.of(),
            List.of(
                new TripParticipant(alice, "Alice", null, null, partyA, "Familie A"),
                new TripParticipant(bob, "Bob", null, null, partyB, "Familie B")
            ),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
            null
        );

        final PartyAccount familyA = accounts.stream()
            .filter(account -> account.partyTenantId().equals(partyA))
            .findFirst()
            .orElseThrow();

        assertThat(familyA.receiptCredits()).isEqualByComparingTo("19.90");
        assertThat(familyA.entries())
            .extracting(PartyAccountEntry::type, PartyAccountEntry::label, PartyAccountEntry::amount)
            .contains(
                tuple(PartyAccountEntryType.RECEIPT_CREDIT, "Supermarkt (2026-07-02)", new BigDecimal("12.50")),
                tuple(PartyAccountEntryType.RECEIPT_CREDIT, "Baeckerei (2026-07-03)", new BigDecimal("7.40"))
            );
    }

    @Test
    void paidAdvancePaymentsAppearWithPaymentDateInTimeline() {
        final UUID partyA = UUID.randomUUID();
        final UUID alice = UUID.randomUUID();
        final LocalDate paidOn = LocalDate.of(2026, 7, 4);

        final List<PartyAccount> accounts = PartyAccounting.calculate(
            List.of(new ParticipantWeighting(alice, BigDecimal.ONE)),
            List.of(),
            List.of(new AdvancePayment(
                new AdvancePaymentId(UUID.randomUUID()), partyA, "Familie A", new BigDecimal("50.00"), true, paidOn, alice
            )),
            List.of(new TripParticipant(alice, "Alice", null, null, partyA, "Familie A")),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
            null
        );

        final PartyAccount familyA = accounts.getFirst();
        assertThat(familyA.entries())
            .extracting(PartyAccountEntry::type, PartyAccountEntry::label, PartyAccountEntry::amount)
            .contains(tuple(PartyAccountEntryType.ADVANCE_PAYMENT, "Familie A bezahlt am 2026-07-04 von Alice", new BigDecimal("50.00")));
    }
}
