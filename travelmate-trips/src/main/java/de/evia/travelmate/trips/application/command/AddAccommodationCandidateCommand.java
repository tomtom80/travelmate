package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddAccommodationCandidateCommand(
    UUID tenantId,
    UUID accommodationPollId,
    String name,
    String url,
    String description,
    List<CreateAccommodationPollCommand.RoomProposalCommand> rooms
) {
}
