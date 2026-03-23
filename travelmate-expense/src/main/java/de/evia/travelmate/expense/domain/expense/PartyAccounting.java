package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.evia.travelmate.expense.domain.trip.TripParticipant;

public final class PartyAccounting {

    private PartyAccounting() {
    }

    public static List<PartyAccount> calculate(final List<ParticipantWeighting> weightings,
                                               final List<Receipt> receipts,
                                               final List<AdvancePayment> advancePayments,
                                               final List<TripParticipant> participants,
                                               final LocalDate tripStartDate,
                                               final LocalDate tripEndDate,
                                               final BigDecimal accommodationTotalPrice) {
        final Map<UUID, UUID> participantToParty = buildParticipantToPartyMap(participants);
        if (participantToParty.isEmpty()) {
            return List.of();
        }

        final Map<UUID, String> partyNames = new LinkedHashMap<>();
        final Map<UUID, List<String>> partyMembers = new LinkedHashMap<>();
        for (final TripParticipant p : participants) {
            if (p.hasPartyInfo()) {
                partyNames.putIfAbsent(p.partyTenantId(), p.partyName());
                partyMembers.computeIfAbsent(p.partyTenantId(), ignored -> new ArrayList<>()).add(p.name());
            }
        }

        final BigDecimal totalWeight = weightings.stream()
            .map(ParticipantWeighting::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal totalReceiptAmount = receipts.stream()
            .map(r -> r.amount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Map<UUID, BigDecimal> receiptCredits = new HashMap<>();
        for (final Receipt receipt : receipts) {
            final UUID partyId = participantToParty.get(receipt.paidBy());
            if (partyId != null) {
                receiptCredits.merge(partyId, receipt.amount().value(), BigDecimal::add);
            }
        }

        final Map<UUID, BigDecimal> fairShare = new HashMap<>();
        for (final ParticipantWeighting weighting : weightings) {
            final UUID partyId = participantToParty.get(weighting.participantId());
            if (partyId != null && totalWeight.signum() > 0) {
                final BigDecimal share = totalReceiptAmount.multiply(weighting.weight())
                    .divide(totalWeight, 2, RoundingMode.HALF_UP);
                fairShare.merge(partyId, share, BigDecimal::add);
            }
        }

        final Map<UUID, BigDecimal> accommodationShareByParticipant = buildAccommodationShareByParticipant(
            weightings, participants, tripStartDate, tripEndDate, accommodationTotalPrice);
        final Map<UUID, TripParticipant> participantsById = new HashMap<>();
        for (final TripParticipant participant : participants) {
            participantsById.put(participant.participantId(), participant);
        }
        final Map<UUID, BigDecimal> weightingByParticipant = new HashMap<>();
        for (final ParticipantWeighting weighting : weightings) {
            weightingByParticipant.put(weighting.participantId(), weighting.weight());
        }
        for (final Map.Entry<UUID, BigDecimal> entry : accommodationShareByParticipant.entrySet()) {
            final UUID partyId = participantToParty.get(entry.getKey());
            if (partyId != null) {
                fairShare.merge(partyId, entry.getValue(), BigDecimal::add);
            }
        }

        final Map<UUID, BigDecimal> advancePaid = new HashMap<>();
        final Map<UUID, BigDecimal> advancePlanned = new HashMap<>();
        for (final AdvancePayment advancePayment : advancePayments) {
            advancePlanned.merge(advancePayment.partyTenantId(), advancePayment.amount(), BigDecimal::add);
            if (advancePayment.paid()) {
                advancePaid.merge(advancePayment.partyTenantId(), advancePayment.amount(), BigDecimal::add);
            }
        }

        return partyNames.entrySet().stream()
            .map(entry -> {
                final UUID partyId = entry.getKey();
                final BigDecimal receiptCredit = receiptCredits.getOrDefault(partyId, BigDecimal.ZERO);
                final BigDecimal plannedAdvances = advancePlanned.getOrDefault(partyId, BigDecimal.ZERO);
                final BigDecimal paidAdvances = advancePaid.getOrDefault(partyId, BigDecimal.ZERO);
                final BigDecimal outstandingAdvances = plannedAdvances.subtract(paidAdvances).max(BigDecimal.ZERO);
                final BigDecimal owed = fairShare.getOrDefault(partyId, BigDecimal.ZERO);
                final BigDecimal currentBalance = receiptCredit.add(paidAdvances).subtract(owed);
                final BigDecimal outstandingAmount = currentBalance.signum() < 0
                    ? currentBalance.abs()
                    : BigDecimal.ZERO;
                final BigDecimal creditAmount = currentBalance.signum() > 0
                    ? currentBalance
                    : BigDecimal.ZERO;
                return new PartyAccount(
                    partyId,
                    entry.getValue(),
                    partyMembers.getOrDefault(partyId, List.of()),
                    buildEntries(
                        partyId,
                        participantToParty,
                        receipts,
                        advancePayments,
                        plannedAdvances,
                        paidAdvances,
                        outstandingAdvances,
                        owed,
                        accommodationTotalPrice,
                        accommodationShareByParticipant,
                        participantsById,
                        weightingByParticipant,
                        tripStartDate,
                        tripEndDate
                    ),
                    receiptCredit,
                    plannedAdvances,
                    paidAdvances,
                    outstandingAdvances,
                    owed,
                    currentBalance,
                    outstandingAmount,
                    creditAmount
                );
            })
            .toList();
    }

    private static List<PartyAccountEntry> buildEntries(final UUID partyId,
                                                        final Map<UUID, UUID> participantToParty,
                                                        final List<Receipt> receipts,
                                                        final List<AdvancePayment> advancePayments,
                                                        final BigDecimal plannedAdvances,
                                                        final BigDecimal paidAdvances,
                                                        final BigDecimal outstandingAdvances,
                                                        final BigDecimal fairShare,
                                                        final BigDecimal accommodationTotalPrice,
                                                        final Map<UUID, BigDecimal> accommodationShareByParticipant,
                                                        final Map<UUID, TripParticipant> participantsById,
                                                        final Map<UUID, BigDecimal> weightingByParticipant,
                                                        final LocalDate tripStartDate,
                                                        final LocalDate tripEndDate) {
        final List<PartyAccountEntry> entries = new ArrayList<>();
        receipts.stream()
            .filter(receipt -> partyId.equals(participantToParty.get(receipt.paidBy())))
            .sorted((left, right) -> left.date().compareTo(right.date()))
            .forEach(receipt -> entries.add(new PartyAccountEntry(
                PartyAccountEntryType.RECEIPT_CREDIT,
                receipt.description() + " (" + receipt.date() + ")",
                receipt.amount().value()
            )));
        if (paidAdvances.signum() > 0) {
            advancePaymentsForParty(partyId, advancePayments, true).forEach(payment ->
                entries.add(new PartyAccountEntry(
                    PartyAccountEntryType.ADVANCE_PAYMENT,
                    paymentLabel(payment, participantsById),
                    payment.amount()
                ))
            );
        }
        if (plannedAdvances.signum() > 0 && outstandingAdvances.signum() > 0) {
            advancePaymentsForParty(partyId, advancePayments, false).forEach(payment ->
                entries.add(new PartyAccountEntry(
                    PartyAccountEntryType.ADVANCE_PAYMENT_DUE,
                    paymentLabel(payment, participantsById),
                    payment.amount().negate()
                ))
            );
        }
        if (fairShare.signum() > 0) {
            if (accommodationTotalPrice != null && accommodationTotalPrice.signum() > 0) {
                accommodationShareByParticipant.entrySet().stream()
                    .filter(entry -> partyId.equals(participantToParty.get(entry.getKey())))
                    .sorted(Comparator.comparing(entry -> {
                        final TripParticipant participant = participantsById.get(entry.getKey());
                        return participant != null ? participant.name() : entry.getKey().toString();
                    }))
                    .forEach(entry -> {
                        final TripParticipant participant = participantsById.get(entry.getKey());
                        final long nights = participant != null
                            ? resolveAccommodationNights(participant, tripStartDate, tripEndDate)
                            : 0;
                        final BigDecimal weighting = weightingByParticipant.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                        final String label = participant != null
                            ? participant.name() + " - " + nights + " Naechte x " + weighting.stripTrailingZeros().toPlainString()
                            : "";
                        entries.add(new PartyAccountEntry(
                            PartyAccountEntryType.ACCOMMODATION_SHARE,
                            label,
                            entry.getValue().negate()
                        ));
                    });
            } else {
                entries.add(new PartyAccountEntry(PartyAccountEntryType.RECEIPT_CREDIT, "", fairShare.negate()));
            }
        }
        return List.copyOf(entries);
    }

    private static List<AdvancePayment> advancePaymentsForParty(final UUID partyId,
                                                                final List<AdvancePayment> advancePayments,
                                                                final boolean paid) {
        return advancePayments.stream()
            .filter(payment -> partyId.equals(payment.partyTenantId()) && payment.paid() == paid)
            .sorted(Comparator.comparing(AdvancePayment::partyName))
            .toList();
    }

    private static String paymentLabel(final AdvancePayment payment,
                                       final Map<UUID, TripParticipant> participantsById) {
        final String marker = payment.markedByParticipantId() != null
            ? participantsById.getOrDefault(
                payment.markedByParticipantId(),
                new TripParticipant(payment.markedByParticipantId(), payment.markedByParticipantId().toString())
            ).name()
            : null;
        if (payment.paid() && payment.paidOn() != null) {
            return marker != null
                ? payment.partyName() + " bezahlt am " + payment.paidOn() + " von " + marker
                : payment.partyName() + " bezahlt am " + payment.paidOn();
        }
        return payment.partyName() + " offen";
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

    private static Map<UUID, BigDecimal> buildAccommodationShareByParticipant(
        final List<ParticipantWeighting> weightings,
        final List<TripParticipant> participants,
        final LocalDate tripStartDate,
        final LocalDate tripEndDate,
        final BigDecimal accommodationTotalPrice) {
        if (accommodationTotalPrice == null || accommodationTotalPrice.signum() <= 0) {
            return Map.of();
        }
        final Map<UUID, TripParticipant> participantsById = new HashMap<>();
        for (final TripParticipant participant : participants) {
            participantsById.put(participant.participantId(), participant);
        }
        final Map<UUID, BigDecimal> accommodationBasis = new LinkedHashMap<>();
        BigDecimal totalBasis = BigDecimal.ZERO;
        for (final ParticipantWeighting weighting : weightings) {
            final TripParticipant participant = participantsById.get(weighting.participantId());
            if (participant == null) {
                continue;
            }
            final long nights = resolveAccommodationNights(participant, tripStartDate, tripEndDate);
            final BigDecimal basis = weighting.weight().multiply(BigDecimal.valueOf(nights));
            accommodationBasis.put(weighting.participantId(), basis);
            totalBasis = totalBasis.add(basis);
        }

        if (totalBasis.signum() <= 0) {
            return Map.of();
        }

        final Map<UUID, BigDecimal> result = new HashMap<>();
        for (final Map.Entry<UUID, BigDecimal> entry : accommodationBasis.entrySet()) {
            final BigDecimal share = accommodationTotalPrice.multiply(entry.getValue())
                .divide(totalBasis, 2, RoundingMode.HALF_UP);
            result.put(entry.getKey(), share);
        }
        return result;
    }

    private static long resolveAccommodationNights(final TripParticipant participant,
                                                   final LocalDate tripStartDate,
                                                   final LocalDate tripEndDate) {
        if (participant.hasStayPeriod()) {
            return Math.max(participant.nights(), 0);
        }
        if (tripStartDate != null && tripEndDate != null) {
            final long tripNights = ChronoUnit.DAYS.between(tripStartDate, tripEndDate);
            return Math.max(tripNights, 0);
        }
        return 0;
    }
}
