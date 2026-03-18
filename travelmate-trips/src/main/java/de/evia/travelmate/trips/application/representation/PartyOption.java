package de.evia.travelmate.trips.application.representation;

import java.util.UUID;

public record PartyOption(
    UUID partyTenantId,
    String partyName,
    int memberCount
) {
}
