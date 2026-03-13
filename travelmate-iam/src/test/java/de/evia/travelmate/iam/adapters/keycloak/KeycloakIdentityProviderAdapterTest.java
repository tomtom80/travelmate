package de.evia.travelmate.iam.adapters.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityProviderAdapterTest {

    private static final String REALM = "travelmate";
    private static final String CREATED_USER_ID = "kc-user-123";
    private static final Email EMAIL = new Email("john@example.com");
    private static final FullName FULL_NAME = new FullName("John", "Doe");
    private static final Password PASSWORD = new Password("secureP4ss!");

    @Mock
    private Keycloak keycloak;

    @Test
    void constructorRejectsNullKeycloak() {
        assertThatThrownBy(() -> new KeycloakIdentityProviderAdapter(null, REALM))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsBlankRealm() {
        assertThatThrownBy(() -> new KeycloakIdentityProviderAdapter(keycloak, "  "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class WhenAdapterIsConfigured {

        @Mock
        private RealmResource realmResource;

        @Mock
        private UsersResource usersResource;

        private KeycloakIdentityProviderAdapter adapter;

        @BeforeEach
        void setUp() {
            when(keycloak.realm(REALM)).thenReturn(realmResource);
            adapter = new KeycloakIdentityProviderAdapter(keycloak, REALM);
        }

        @Test
        void createUserReturnsKeycloakUserId() {
            when(realmResource.users()).thenReturn(usersResource);
            final Response response = mockSuccessResponse();
            when(usersResource.create(any())).thenReturn(response);

            final KeycloakUserId result = adapter.createUser(EMAIL, FULL_NAME, PASSWORD);

            assertThat(result.value()).isEqualTo(CREATED_USER_ID);
        }

        @Test
        void createUserSetsEmailAndName() {
            when(realmResource.users()).thenReturn(usersResource);
            final Response response = mockSuccessResponse();
            when(usersResource.create(any())).thenReturn(response);

            adapter.createUser(EMAIL, FULL_NAME, PASSWORD);

            final var captor = ArgumentCaptor.forClass(UserRepresentation.class);
            verify(usersResource).create(captor.capture());

            final var user = captor.getValue();
            assertThat(user.getEmail()).isEqualTo("john@example.com");
            assertThat(user.getUsername()).isEqualTo("john@example.com");
            assertThat(user.getFirstName()).isEqualTo("John");
            assertThat(user.getLastName()).isEqualTo("Doe");
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getRequiredActions()).isEmpty();
        }

        @Test
        void createUserThrowsOnConflict() {
            when(realmResource.users()).thenReturn(usersResource);
            final Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(409);
            when(response.readEntity(String.class)).thenReturn("User exists");
            when(usersResource.create(any())).thenReturn(response);

            assertThatThrownBy(() -> adapter.createUser(
                new Email("existing@example.com"),
                FULL_NAME,
                PASSWORD
            )).isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("existing@example.com");
        }

        @Test
        void createUserThrowsWhenLocationHeaderMissing() {
            when(realmResource.users()).thenReturn(usersResource);
            final Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getLocation()).thenReturn(null);
            when(usersResource.create(any())).thenReturn(response);

            assertThatThrownBy(() -> adapter.createUser(EMAIL, FULL_NAME, PASSWORD))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("no Location header");
        }

        @Test
        void assignRoleDelegatesToKeycloak() {
            when(realmResource.users()).thenReturn(usersResource);
            final UserResource userResource = mock(UserResource.class);
            final RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
            final RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
            final RolesResource rolesResource = mock(RolesResource.class);
            final RoleResource roleResource = mock(RoleResource.class);

            when(usersResource.get(CREATED_USER_ID)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get("organizer")).thenReturn(roleResource);

            final RoleRepresentation role = new RoleRepresentation();
            role.setName("organizer");
            when(roleResource.toRepresentation()).thenReturn(role);

            adapter.assignRole(new KeycloakUserId(CREATED_USER_ID), "organizer");

            verify(roleScopeResource).add(List.of(role));
        }

        @Test
        void assignRoleThrowsWhenRoleNotFound() {
            final RolesResource rolesResource = mock(RolesResource.class);
            final RoleResource roleResource = mock(RoleResource.class);
            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get("nonexistent")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenThrow(new NotFoundException("Role not found"));

            assertThatThrownBy(() -> adapter.assignRole(new KeycloakUserId(CREATED_USER_ID), "nonexistent"))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("nonexistent")
                .hasMessageContaining("not found");
        }

        @Test
        void deleteUserDelegatesToKeycloak() {
            when(realmResource.users()).thenReturn(usersResource);
            final UserResource userResource = mock(UserResource.class);
            when(usersResource.get(CREATED_USER_ID)).thenReturn(userResource);

            adapter.deleteUser(new KeycloakUserId(CREATED_USER_ID));

            verify(userResource).remove();
        }

        @Test
        void deleteUserThrowsWhenUserNotFound() {
            when(realmResource.users()).thenReturn(usersResource);
            final UserResource userResource = mock(UserResource.class);
            when(usersResource.get("nonexistent")).thenReturn(userResource);
            doThrow(new NotFoundException("User not found")).when(userResource).remove();

            assertThatThrownBy(() -> adapter.deleteUser(new KeycloakUserId("nonexistent")))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("nonexistent")
                .hasMessageContaining("not found");
        }

        private Response mockSuccessResponse() {
            final Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getLocation()).thenReturn(URI.create("http://localhost/users/" + CREATED_USER_ID));
            return response;
        }
    }
}
