package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record CastAccommodationVoteCommand(
    UUID tenantId,
    UUID accommodationPollId,
    UUID voterId,
    UUID selectedCandidateId
) {
}
