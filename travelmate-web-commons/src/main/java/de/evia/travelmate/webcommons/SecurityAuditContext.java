package de.evia.travelmate.webcommons;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityAuditContext {

    private SecurityAuditContext() {}

    public static UUID currentActorId() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }
        final String subject = jwtAuth.getToken().getSubject();
        try {
            return UUID.fromString(subject);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}
