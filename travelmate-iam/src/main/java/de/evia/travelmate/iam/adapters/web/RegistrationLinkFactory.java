package de.evia.travelmate.iam.adapters.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegistrationLinkFactory {

    private final String publicBaseUrl;

    public RegistrationLinkFactory(@Value("${travelmate.public-url:http://localhost:8080}") final String publicBaseUrl) {
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    public String registrationLink(final String tokenValue) {
        return publicBaseUrl + "/iam/register?token=" + tokenValue;
    }

    private String stripTrailingSlash(final String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
