package de.evia.travelmate.iam.domain;

import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.tenant.Description;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;

public final class IamTestFixtures {

    private IamTestFixtures() {
    }

    public static final TenantId TENANT_ID = new TenantId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    public static final AccountId ACCOUNT_ID = new AccountId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"));

    public static TenantName tenantName() {
        return new TenantName("Reisegruppe Alpen");
    }

    public static Description description() {
        return new Description("Eine Reisegruppe fuer Alpentouren");
    }

    public static Tenant tenant() {
        return new Tenant(TENANT_ID, tenantName(), description());
    }

    public static KeycloakUserId keycloakUserId() {
        return new KeycloakUserId("kc-user-" + UUID.randomUUID());
    }

    public static Username username() {
        return new Username("testuser");
    }

    public static Email email() {
        return new Email("test@example.com");
    }

    public static FullName fullName() {
        return new FullName("Max", "Mustermann");
    }

    public static Account account() {
        return new Account(ACCOUNT_ID, TENANT_ID, keycloakUserId(), username(), email(), fullName(), null);
    }

    public static Account registeredAccount() {
        return Account.register(TENANT_ID, keycloakUserId(), username(), email(), fullName());
    }

    public static Dependent dependent() {
        return Dependent.add(TENANT_ID, ACCOUNT_ID, new FullName("Lena", "Mustermann"));
    }
}
