package de.evia.travelmate.trips.domain.travelparty;

import java.util.UUID;

public record TravelPartyDependent(
    UUID dependentId,
    UUID guardianMemberId,
    String firstName,
    String lastName
) {
}
