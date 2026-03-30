package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record AddAccommodationCandidateCommand(
    UUID tenantId,
    UUID accommodationPollId,
    String name,
    String url,
    String description
) {
}
