package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodationpoll.Amenity;

public record CreateAccommodationPollCommand(
    UUID tenantId,
    UUID tripId,
    List<CandidateProposalCommand> candidates
) {
    public record CandidateProposalCommand(String name, String url, String description,
                                           List<RoomProposalCommand> rooms, Set<Amenity> amenities) {
    }

    public record RoomProposalCommand(String name, int bedCount,
                                      BigDecimal pricePerNight, String bedDescription) {
    }
}
