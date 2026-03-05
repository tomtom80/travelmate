package de.evia.travelmate.iam.domain.account;

public interface IdentityProviderService {

    KeycloakUserId createUser(Email email, FullName fullName, Password password);

    KeycloakUserId createInvitedUser(Email email, FullName fullName);

    void sendActionsEmail(KeycloakUserId userId);

    void assignRole(KeycloakUserId userId, String roleName);

    void deleteUser(KeycloakUserId userId);
}
