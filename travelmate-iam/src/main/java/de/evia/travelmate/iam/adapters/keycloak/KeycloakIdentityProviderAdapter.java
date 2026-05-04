package de.evia.travelmate.iam.adapters.keycloak;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;

public class KeycloakIdentityProviderAdapter implements IdentityProviderService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakIdentityProviderAdapter(final Keycloak keycloak, final String realm) {
        argumentIsNotNull(keycloak, "keycloak");
        argumentIsNotBlank(realm, "realm");
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public KeycloakUserId createUser(final Email email, final FullName fullName, final Password password) {
        final UserRepresentation user = buildUserRepresentation(email, fullName);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of());

        final CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password.value());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return createKeycloakUser(user, email);
    }

    @Override
    public KeycloakUserId createInvitedUser(final Email email, final FullName fullName) {
        final UserRepresentation user = buildUserRepresentation(email, fullName);
        user.setRequiredActions(List.of());

        final CredentialRepresentation tempCredential = new CredentialRepresentation();
        tempCredential.setType(CredentialRepresentation.PASSWORD);
        tempCredential.setValue(java.util.UUID.randomUUID().toString());
        tempCredential.setTemporary(true);
        user.setCredentials(List.of(tempCredential));

        return createKeycloakUser(user, email);
    }

    @Override
    public void setPassword(final KeycloakUserId userId, final Password password) {
        try {
            final CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password.value());
            credential.setTemporary(false);
            realmResource().users().get(userId.value()).resetPassword(credential);
        } catch (final Exception e) {
            throw new IdentityProviderException(
                "Failed to set password for user " + userId.value(), e);
        }
    }

    @Override
    public void setEmailVerified(final KeycloakUserId userId, final boolean verified) {
        try {
            final UserRepresentation user = realmResource().users().get(userId.value()).toRepresentation();
            user.setEmailVerified(verified);
            realmResource().users().get(userId.value()).update(user);
        } catch (final Exception e) {
            throw new IdentityProviderException(
                "Failed to set email verified for user " + userId.value(), e);
        }
    }

    @Override
    public void sendVerificationEmail(final KeycloakUserId userId) {
        try {
            realmResource().users().get(userId.value())
                .executeActionsEmail(List.of("VERIFY_EMAIL"));
        } catch (final Exception e) {
            throw new IdentityProviderException(
                "Failed to send verification email for user " + userId.value(), e);
        }
    }

    @Override
    public boolean hasPasswordCredential(final KeycloakUserId userId) {
        argumentIsNotNull(userId, "userId");
        try {
            return realmResource().users().get(userId.value()).credentials().stream()
                .anyMatch(credential -> CredentialRepresentation.PASSWORD.equals(credential.getType()));
        } catch (final Exception e) {
            throw new IdentityProviderException(
                "Failed to read credentials for user " + userId.value(), e);
        }
    }

    @Override
    public void sendUpdatePasswordEmail(final KeycloakUserId userId,
                                        final URI redirectUri,
                                        final String clientId) {
        argumentIsNotNull(userId, "userId");
        argumentIsNotNull(redirectUri, "redirectUri");
        argumentIsNotBlank(clientId, "clientId");
        try {
            realmResource().users().get(userId.value())
                .executeActionsEmail(clientId, redirectUri.toString(), List.of("UPDATE_PASSWORD"));
        } catch (final Exception e) {
            throw new IdentityProviderException(
                "Failed to send update-password email for user " + userId.value(), e);
        }
    }

    @Override
    public void assignRole(final KeycloakUserId userId, final String roleName) {
        try {
            final RoleResource roleResource = realmResource().roles().get(roleName);
            final RoleRepresentation role = roleResource.toRepresentation();
            realmResource().users().get(userId.value())
                .roles()
                .realmLevel()
                .add(List.of(role));
        } catch (final jakarta.ws.rs.NotFoundException e) {
            throw new IdentityProviderException(
                "Role '" + roleName + "' not found in Keycloak realm " + realm, e);
        }
    }

    @Override
    public void deleteUser(final KeycloakUserId userId) {
        try {
            realmResource().users().get(userId.value()).remove();
        } catch (final jakarta.ws.rs.NotFoundException e) {
            throw new IdentityProviderException(
                "User " + userId.value() + " not found in Keycloak realm " + realm, e);
        }
    }

    private UserRepresentation buildUserRepresentation(final Email email, final FullName fullName) {
        final UserRepresentation user = new UserRepresentation();
        user.setEmail(email.value());
        user.setUsername(email.value());
        user.setFirstName(fullName.firstName());
        user.setLastName(fullName.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        return user;
    }

    private KeycloakUserId createKeycloakUser(final UserRepresentation user, final Email email) {
        try (final Response response = realmResource().users().create(user)) {
            if (response.getStatus() != 201) {
                throw new IdentityProviderException(
                    "Failed to create Keycloak user for " + email.value()
                        + ": " + response.readEntity(String.class));
            }

            final URI location = response.getLocation();
            if (location == null) {
                throw new IdentityProviderException(
                    "Keycloak returned 201 but no Location header for user " + email.value());
            }

            final String locationPath = location.getPath();
            final String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
            return new KeycloakUserId(userId);
        }
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }
}
