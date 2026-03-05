package de.evia.travelmate.iam.domain.account;

public interface IdentityProviderService {

    KeycloakUserId createUser(Email email, FullName fullName, String password);

    void assignRole(KeycloakUserId userId, String roleName);

    void deleteUser(KeycloakUserId userId);
}
