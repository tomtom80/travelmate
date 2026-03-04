package de.evia.travelmate.iam.application.representation;

import java.util.UUID;

import de.evia.travelmate.iam.domain.dependent.Dependent;

public record DependentRepresentation(
    UUID dependentId,
    UUID tenantId,
    UUID guardianAccountId,
    String firstName,
    String lastName
) {

    public DependentRepresentation(final Dependent dependent) {
        this(
            dependent.dependentId().value(),
            dependent.tenantId().value(),
            dependent.guardianAccountId().value(),
            dependent.fullName().firstName(),
            dependent.fullName().lastName()
        );
    }
}
