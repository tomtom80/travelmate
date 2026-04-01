package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record SelectAccommodationCandidateCommand(
    UUID tenantId,
    UUID tripId,
    UUID accommodationPollId,
    UUID candidateId
) {
}
