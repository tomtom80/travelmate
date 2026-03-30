package de.evia.travelmate.trips.domain.datepoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class DateVote {

    private final DateVoteId dateVoteId;
    private final UUID voterId;
    private final Set<DateOptionId> selectedOptionIds;

    public DateVote(final DateVoteId dateVoteId, final UUID voterId, final Set<DateOptionId> selectedOptionIds) {
        argumentIsNotNull(dateVoteId, "dateVoteId");
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(selectedOptionIds, "selectedOptionIds");
        argumentIsTrue(!selectedOptionIds.isEmpty(), "At least one option must be selected.");
        this.dateVoteId = dateVoteId;
        this.voterId = voterId;
        this.selectedOptionIds = new LinkedHashSet<>(selectedOptionIds);
    }

    public void changeSelection(final Set<DateOptionId> newSelection) {
        argumentIsNotNull(newSelection, "newSelection");
        argumentIsTrue(!newSelection.isEmpty(), "At least one option must be selected.");
        this.selectedOptionIds.clear();
        this.selectedOptionIds.addAll(newSelection);
    }

    public DateVoteId dateVoteId() {
        return dateVoteId;
    }

    public UUID voterId() {
        return voterId;
    }

    public Set<DateOptionId> selectedOptionIds() {
        return Collections.unmodifiableSet(selectedOptionIds);
    }
}
