package de.evia.travelmate.iam.domain.account;

import java.net.URI;

public interface IdentityProviderService {

    KeycloakUserId createUser(Email email, FullName fullName, Password password);

    KeycloakUserId createInvitedUser(Email email, FullName fullName);

    void setPassword(KeycloakUserId userId, Password password);

    void setEmailVerified(KeycloakUserId userId, boolean verified);

    void sendVerificationEmail(KeycloakUserId userId);

    boolean hasPasswordCredential(KeycloakUserId userId);

    void sendUpdatePasswordEmail(KeycloakUserId userId, URI redirectUri, String clientId);

    void assignRole(KeycloakUserId userId, String roleName);

    void deleteUser(KeycloakUserId userId);
}
