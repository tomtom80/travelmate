package de.evia.travelmate.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

import java.net.URI;

@Configuration
public class SecurityConfig {

    @Value("${keycloak.end-session-uri:http://localhost:7082/realms/travelmate/protocol/openid-connect/logout}")
    private String keycloakEndSessionUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
        final var successHandler = new RedirectServerAuthenticationSuccessHandler("/iam/dashboard");

        final var logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(
            URI.create(keycloakEndSessionUri + "?post_logout_redirect_uri=http://localhost:8080&client_id=travelmate-gateway"));

        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/", "/actuator/health/**", "/iam/", "/iam/signup", "/iam/signup/**", "/iam/css/**", "/iam/images/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authenticationSuccessHandler(successHandler)
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler)
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
