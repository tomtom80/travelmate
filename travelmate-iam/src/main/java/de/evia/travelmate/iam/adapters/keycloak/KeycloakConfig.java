package de.evia.travelmate.iam.adapters.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.evia.travelmate.iam.domain.account.IdentityProviderService;

@Configuration
@Profile("!test")
public class KeycloakConfig {

    @Bean
    public Keycloak keycloakAdminClient(
        @Value("${keycloak.admin.server-url}") final String serverUrl,
        @Value("${keycloak.admin.realm}") final String adminRealm,
        @Value("${keycloak.admin.client-id}") final String clientId,
        @Value("${keycloak.admin.username}") final String username,
        @Value("${keycloak.admin.password}") final String password) {
        return KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm(adminRealm)
            .clientId(clientId)
            .username(username)
            .password(password)
            .build();
    }

    @Bean
    public IdentityProviderService identityProviderService(
        final Keycloak keycloak,
        @Value("${keycloak.admin.target-realm}") final String targetRealm) {
        return new KeycloakIdentityProviderAdapter(keycloak, targetRealm);
    }
}
