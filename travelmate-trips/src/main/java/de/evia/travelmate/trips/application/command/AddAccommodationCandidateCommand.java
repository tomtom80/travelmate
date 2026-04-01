package de.evia.travelmate.trips.application.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodationpoll.Amenity;

public record AddAccommodationCandidateCommand(
    UUID tenantId,
    UUID accommodationPollId,
    String name,
    String url,
    String description,
    List<CreateAccommodationPollCommand.RoomProposalCommand> rooms,
    Set<Amenity> amenities
) {
}
