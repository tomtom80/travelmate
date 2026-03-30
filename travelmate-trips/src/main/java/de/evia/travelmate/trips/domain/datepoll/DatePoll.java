package de.evia.travelmate.trips.domain.datepoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

public class DatePoll extends AggregateRoot {

    private final DatePollId datePollId;
    private final TenantId tenantId;
    private final TripId tripId;
    private final List<DateOption> options;
    private final List<DateVote> votes;
    private PollStatus status;
    private DateOptionId confirmedOptionId;

    public DatePoll(final DatePollId datePollId,
                    final TenantId tenantId,
                    final TripId tripId,
                    final PollStatus status,
                    final List<DateOption> options,
                    final List<DateVote> votes,
                    final DateOptionId confirmedOptionId) {
        argumentIsNotNull(datePollId, "datePollId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(status, "status");
        argumentIsNotNull(options, "options");
        argumentIsNotNull(votes, "votes");
        this.datePollId = datePollId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
        this.options = new ArrayList<>(options);
        this.votes = new ArrayList<>(votes);
        this.confirmedOptionId = confirmedOptionId;
    }

    public static DatePoll create(final TenantId tenantId,
                                  final TripId tripId,
                                  final List<DateRange> dateRanges) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(dateRanges, "dateRanges");
        argumentIsTrue(dateRanges.size() >= 2,
            "A date poll requires at least 2 date options.");

        final List<DateOption> options = dateRanges.stream()
            .map(DateOption::new)
            .toList();

        return new DatePoll(
            new DatePollId(UUID.randomUUID()),
            tenantId, tripId,
            PollStatus.OPEN,
            options,
            List.of(),
            null
        );
    }

    public DateOptionId addOption(final DateRange dateRange) {
        assertOpen();
        argumentIsNotNull(dateRange, "dateRange");
        final DateOption option = new DateOption(dateRange);
        options.add(option);
        return option.dateOptionId();
    }

    public void removeOption(final DateOptionId optionId) {
        assertOpen();
        argumentIsNotNull(optionId, "optionId");
        final DateOption option = findOption(optionId);
        for (final DateVote vote : votes) {
            vote.selectedOptionIds().stream()
                .filter(id -> id.equals(optionId))
                .findFirst()
                .ifPresent(id -> {
                    throw new IllegalArgumentException(
                        "Cannot remove option " + optionId.value()
                            + " because it has votes. Remove votes first or cancel the poll.");
                });
        }
        options.remove(option);
    }

    public DateVoteId castVote(final UUID voterId, final Set<DateOptionId> selectedOptionIds) {
        assertOpen();
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(selectedOptionIds, "selectedOptionIds");
        argumentIsTrue(!selectedOptionIds.isEmpty(), "At least one option must be selected.");
        validateOptionIds(selectedOptionIds);

        if (findVoteByVoter(voterId) != null) {
            throw new IllegalArgumentException(
                "Voter " + voterId + " already has a vote. Use changeVote() instead.");
        }

        final DateVote vote = new DateVote(
            new DateVoteId(UUID.randomUUID()),
            voterId,
            selectedOptionIds
        );
        votes.add(vote);
        return vote.dateVoteId();
    }

    public void changeVote(final UUID voterId, final Set<DateOptionId> newSelection) {
        assertOpen();
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(newSelection, "newSelection");
        argumentIsTrue(!newSelection.isEmpty(), "At least one option must be selected.");
        validateOptionIds(newSelection);

        final DateVote vote = findVoteByVoter(voterId);
        if (vote == null) {
            throw new IllegalArgumentException(
                "Voter " + voterId + " has not voted yet. Use castVote() first.");
        }
        vote.changeSelection(newSelection);
    }

    public DateRange confirm(final DateOptionId optionId) {
        assertOpen();
        argumentIsNotNull(optionId, "optionId");
        final DateOption option = findOption(optionId);
        this.confirmedOptionId = optionId;
        this.status = PollStatus.CONFIRMED;
        return option.dateRange();
    }

    public void cancel() {
        assertOpen();
        this.status = PollStatus.CANCELLED;
    }

    public long voteCountForOption(final DateOptionId optionId) {
        return votes.stream()
            .filter(v -> v.selectedOptionIds().contains(optionId))
            .count();
    }

    private DateOption findOption(final DateOptionId optionId) {
        return options.stream()
            .filter(o -> o.dateOptionId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Date option " + optionId.value() + " not found in this poll."));
    }

    private DateVote findVoteByVoter(final UUID voterId) {
        return votes.stream()
            .filter(v -> v.voterId().equals(voterId))
            .findFirst()
            .orElse(null);
    }

    private void validateOptionIds(final Set<DateOptionId> optionIds) {
        for (final DateOptionId optionId : optionIds) {
            findOption(optionId);
        }
    }

    private void assertOpen() {
        if (this.status != PollStatus.OPEN) {
            throw new IllegalStateException(
                "Date poll is " + this.status + ", expected OPEN.");
        }
    }

    public DatePollId datePollId() {
        return datePollId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public PollStatus status() {
        return status;
    }

    public List<DateOption> options() {
        return Collections.unmodifiableList(options);
    }

    public List<DateVote> votes() {
        return Collections.unmodifiableList(votes);
    }

    public DateOptionId confirmedOptionId() {
        return confirmedOptionId;
    }
}
