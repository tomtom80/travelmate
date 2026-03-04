package de.evia.travelmate.iam.domain.account;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.AccountRegistered;

public class Account extends AggregateRoot {

    private final AccountId accountId;
    private final TenantId tenantId;
    private final KeycloakUserId keycloakUserId;
    private final Username username;
    private final Email email;
    private final FullName fullName;

    public Account(final AccountId accountId,
                   final TenantId tenantId,
                   final KeycloakUserId keycloakUserId,
                   final Username username,
                   final Email email,
                   final FullName fullName) {
        argumentIsNotNull(accountId, "accountId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(keycloakUserId, "keycloakUserId");
        argumentIsNotNull(username, "username");
        argumentIsNotNull(email, "email");
        argumentIsNotNull(fullName, "fullName");
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    public static Account register(final TenantId tenantId,
                                   final KeycloakUserId keycloakUserId,
                                   final Username username,
                                   final Email email,
                                   final FullName fullName) {
        final Account account = new Account(
            new AccountId(UUID.randomUUID()),
            tenantId,
            keycloakUserId,
            username,
            email,
            fullName
        );
        account.registerEvent(new AccountRegistered(
            tenantId.value(),
            account.accountId.value(),
            username.value(),
            fullName.firstName(),
            fullName.lastName(),
            email.value(),
            LocalDate.now()
        ));
        return account;
    }

    public AccountId accountId() {
        return accountId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public KeycloakUserId keycloakUserId() {
        return keycloakUserId;
    }

    public Username username() {
        return username;
    }

    public Email email() {
        return email;
    }

    public FullName fullName() {
        return fullName;
    }
}
