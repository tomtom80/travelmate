package de.evia.travelmate.iam.adapters.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityProviderAdapterTest {

    private static final String REALM = "travelmate";
    private static final String CREATED_USER_ID = "kc-user-123";

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        adapter = new KeycloakIdentityProviderAdapter(keycloak, REALM);
    }

    @Test
    void createUserReturnsKeycloakUserId() {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://localhost/users/" + CREATED_USER_ID));
        when(usersResource.create(any())).thenReturn(response);

        final KeycloakUserId result = adapter.createUser(
            new Email("john@example.com"),
            new FullName("John", "Doe"),
            "secureP4ss!"
        );

        assertThat(result.value()).isEqualTo(CREATED_USER_ID);
    }

    @Test
    void createUserSetsEmailAndName() {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://localhost/users/" + CREATED_USER_ID));
        when(usersResource.create(any())).thenReturn(response);

        adapter.createUser(
            new Email("john@example.com"),
            new FullName("John", "Doe"),
            "secureP4ss!"
        );

        final var captor = ArgumentCaptor.forClass(org.keycloak.representations.idm.UserRepresentation.class);
        verify(usersResource).create(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getUsername()).isEqualTo("john@example.com");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void createUserThrowsOnConflict() {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(409);
        when(response.readEntity(String.class)).thenReturn("User exists");
        when(usersResource.create(any())).thenReturn(response);

        assertThatThrownBy(() -> adapter.createUser(
            new Email("existing@example.com"),
            new FullName("Jane", "Doe"),
            "secureP4ss!"
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("existing@example.com");
    }

    @Test
    void assignRoleDelegatesToKeycloak() {
        final UserResource userResource = mock(UserResource.class);
        final RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        final RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
        final RolesResource rolesResource = mock(RolesResource.class);

        when(usersResource.get(CREATED_USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(realmResource.roles()).thenReturn(rolesResource);

        final RoleRepresentation role = new RoleRepresentation();
        role.setName("organizer");
        when(rolesResource.get("organizer")).thenReturn(mock(org.keycloak.admin.client.resource.RoleResource.class));
        when(rolesResource.get("organizer").toRepresentation()).thenReturn(role);

        adapter.assignRole(new KeycloakUserId(CREATED_USER_ID), "organizer");

        verify(roleScopeResource).add(List.of(role));
    }

    @Test
    void deleteUserDelegatesToKeycloak() {
        final UserResource userResource = mock(UserResource.class);
        when(usersResource.get(CREATED_USER_ID)).thenReturn(userResource);

        adapter.deleteUser(new KeycloakUserId(CREATED_USER_ID));

        verify(userResource).remove();
    }
}
