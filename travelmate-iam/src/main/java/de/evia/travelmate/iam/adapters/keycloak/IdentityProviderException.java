package de.evia.travelmate.iam.adapters.keycloak;

public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(final String message) {
        super(message);
    }

    public IdentityProviderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
