package de.evia.travelmate.trips.application.command;

import java.util.Set;
import java.util.UUID;

public record CastDateVoteCommand(
    UUID tenantId,
    UUID datePollId,
    UUID voterId,
    Set<UUID> selectedOptionIds
) {
}
