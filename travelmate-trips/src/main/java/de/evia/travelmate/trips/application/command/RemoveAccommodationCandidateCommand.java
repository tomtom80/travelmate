package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RemoveAccommodationCandidateCommand(
    UUID tenantId,
    UUID accommodationPollId,
    UUID candidateId
) {
}
