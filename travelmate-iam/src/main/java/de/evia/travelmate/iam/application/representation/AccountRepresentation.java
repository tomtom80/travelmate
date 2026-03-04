package de.evia.travelmate.iam.application.representation;

import java.util.UUID;

import de.evia.travelmate.iam.domain.account.Account;

public record AccountRepresentation(
    UUID accountId,
    UUID tenantId,
    String username,
    String email,
    String firstName,
    String lastName
) {

    public AccountRepresentation(final Account account) {
        this(
            account.accountId().value(),
            account.tenantId().value(),
            account.username().value(),
            account.email().value(),
            account.fullName().firstName(),
            account.fullName().lastName()
        );
    }
}
