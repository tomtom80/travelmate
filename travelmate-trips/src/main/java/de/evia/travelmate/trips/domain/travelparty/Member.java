package de.evia.travelmate.trips.domain.travelparty;

import java.util.UUID;

public record Member(
    UUID memberId,
    String email,
    String firstName,
    String lastName
) {
}
