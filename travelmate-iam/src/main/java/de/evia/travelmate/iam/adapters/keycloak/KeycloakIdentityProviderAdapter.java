package de.evia.travelmate.iam.adapters.keycloak;

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

public class KeycloakIdentityProviderAdapter implements IdentityProviderService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakIdentityProviderAdapter(final Keycloak keycloak, final String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public KeycloakUserId createUser(final Email email, final FullName fullName, final String password) {
        final UserRepresentation user = new UserRepresentation();
        user.setEmail(email.value());
        user.setUsername(email.value());
        user.setFirstName(fullName.firstName());
        user.setLastName(fullName.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        final CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        final Response response = realmResource().users().create(user);
        if (response.getStatus() != 201) {
            throw new IllegalStateException(
                "Failed to create Keycloak user for " + email.value()
                    + ": " + response.readEntity(String.class));
        }

        final String locationPath = response.getLocation().getPath();
        final String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
        return new KeycloakUserId(userId);
    }

    @Override
    public void assignRole(final KeycloakUserId userId, final String roleName) {
        final RoleResource roleResource = realmResource().roles().get(roleName);
        final RoleRepresentation role = roleResource.toRepresentation();
        realmResource().users().get(userId.value())
            .roles()
            .realmLevel()
            .add(List.of(role));
    }

    @Override
    public void deleteUser(final KeycloakUserId userId) {
        realmResource().users().get(userId.value()).remove();
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }
}
