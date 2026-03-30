package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record ConfirmAccommodationPollCommand(
    UUID tenantId,
    UUID accommodationPollId,
    UUID confirmedCandidateId
) {
}
