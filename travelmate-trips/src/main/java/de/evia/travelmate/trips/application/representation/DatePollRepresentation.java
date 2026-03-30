package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.evia.travelmate.trips.domain.datepoll.DateOption;
import de.evia.travelmate.trips.domain.datepoll.DatePoll;
import de.evia.travelmate.trips.domain.datepoll.DateVote;

public record DatePollRepresentation(
    UUID datePollId,
    UUID tenantId,
    UUID tripId,
    String status,
    UUID confirmedOptionId,
    List<DateOptionRepresentation> options,
    List<DateVoteRepresentation> votes
) {

    public DatePollRepresentation(final DatePoll poll) {
        this(
            poll.datePollId().value(),
            poll.tenantId().value(),
            poll.tripId().value(),
            poll.status().name(),
            poll.confirmedOptionId() != null ? poll.confirmedOptionId().value() : null,
            poll.options().stream().map(o -> new DateOptionRepresentation(o, poll)).toList(),
            poll.votes().stream().map(DateVoteRepresentation::new).toList()
        );
    }

    public record DateOptionRepresentation(
        UUID dateOptionId,
        LocalDate startDate,
        LocalDate endDate,
        long voteCount
    ) {
        public DateOptionRepresentation(final DateOption option, final DatePoll poll) {
            this(
                option.dateOptionId().value(),
                option.dateRange().startDate(),
                option.dateRange().endDate(),
                poll.voteCountForOption(option.dateOptionId())
            );
        }
    }

    public record DateVoteRepresentation(
        UUID dateVoteId,
        UUID voterId,
        Set<UUID> selectedOptionIds
    ) {
        public DateVoteRepresentation(final DateVote vote) {
            this(
                vote.dateVoteId().value(),
                vote.voterId(),
                vote.selectedOptionIds().stream()
                    .map(id -> id.value())
                    .collect(Collectors.toSet())
            );
        }
    }
}
