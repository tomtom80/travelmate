package de.evia.travelmate.iam.domain.account;

public interface IdentityProviderService {

    KeycloakUserId createUser(Email email, FullName fullName, Password password);

    KeycloakUserId createInvitedUser(Email email, FullName fullName);

    void setPassword(KeycloakUserId userId, Password password);

    void setEmailVerified(KeycloakUserId userId, boolean verified);

    void sendVerificationEmail(KeycloakUserId userId);

    void assignRole(KeycloakUserId userId, String roleName);

    void deleteUser(KeycloakUserId userId);
}
