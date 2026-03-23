package de.evia.travelmate.trips.application.representation;

import java.util.UUID;

public record PartyParticipantOption(
    UUID participantId,
    String displayName
) {
}
