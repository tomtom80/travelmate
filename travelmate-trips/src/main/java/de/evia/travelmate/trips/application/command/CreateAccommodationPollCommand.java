package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateAccommodationPollCommand(
    UUID tenantId,
    UUID tripId,
    List<CandidateProposalCommand> candidates
) {
    public record CandidateProposalCommand(String name, String url, String description,
                                           List<RoomProposalCommand> rooms) {
    }

    public record RoomProposalCommand(String name, int bedCount,
                                      BigDecimal pricePerNight, String features) {
    }
}
