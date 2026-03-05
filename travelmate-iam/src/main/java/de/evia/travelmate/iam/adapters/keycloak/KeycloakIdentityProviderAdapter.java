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
        final UserRepresentation user = new UserRepresentation();
        user.setEmail(email.value());
        user.setUsername(email.value());
        user.setFirstName(fullName.firstName());
        user.setLastName(fullName.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        final CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password.value());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

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

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }
}
