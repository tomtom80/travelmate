package de.evia.travelmate.trips.domain.travelparty;

import java.time.LocalDate;
import java.util.UUID;

public record Member(
    UUID memberId,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth
) {
    public Member(final UUID memberId, final String email, final String firstName, final String lastName) {
        this(memberId, email, firstName, lastName, null);
    }
}
