package de.evia.travelmate.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    @Value("${keycloak.end-session-uri}")
    private String keycloakEndSessionUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
        final var successHandler = new RedirectServerAuthenticationSuccessHandler("/iam/dashboard");

        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/", "/actuator/health/**", "/manifest.json", "/iam/", "/iam/signup", "/iam/signup/**", "/iam/register", "/iam/register/**", "/iam/css/**", "/iam/images/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authenticationSuccessHandler(successHandler)
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessHandler(keycloakLogoutSuccessHandler())
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    private ServerLogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return (final WebFilterExchange exchange, final Authentication authentication) -> {
            final String baseUrl = exchange.getExchange().getRequest().getURI().getScheme()
                + "://" + exchange.getExchange().getRequest().getURI().getAuthority();

            final var uriBuilder = UriComponentsBuilder.fromUriString(keycloakEndSessionUri)
                .queryParam("post_logout_redirect_uri", baseUrl + "/iam/")
                .queryParam("client_id", "travelmate-gateway");

            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                final String idToken = oidcUser.getIdToken().getTokenValue();
                uriBuilder.queryParam("id_token_hint", idToken);
            }

            final URI redirectUri = uriBuilder.build().toUri();
            exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
            exchange.getExchange().getResponse().getHeaders().setLocation(redirectUri);
            return exchange.getExchange().getResponse().setComplete();
        };
    }
}
