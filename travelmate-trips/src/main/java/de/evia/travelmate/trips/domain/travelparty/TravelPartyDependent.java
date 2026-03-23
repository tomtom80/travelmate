package de.evia.travelmate.trips.domain.travelparty;

import java.time.LocalDate;
import java.util.UUID;

public record TravelPartyDependent(
    UUID dependentId,
    UUID guardianMemberId,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {
    public TravelPartyDependent(final UUID dependentId, final UUID guardianMemberId,
                                final String firstName, final String lastName) {
        this(dependentId, guardianMemberId, firstName, lastName, null);
    }
}
