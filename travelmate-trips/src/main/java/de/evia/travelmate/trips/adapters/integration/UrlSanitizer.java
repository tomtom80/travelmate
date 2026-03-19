package de.evia.travelmate.trips.adapters.integration;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

final class UrlSanitizer {

    private UrlSanitizer() {
    }

    static String validate(final String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }

        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }

        final String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP or HTTPS URLs are allowed.");
        }

        final String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host.");
        }

        checkHostNotPrivate(host);

        return url;
    }

    private static void checkHostNotPrivate(final String host) {
        try {
            final InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("URL is not reachable: " + host);
            }
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("URL is not reachable: " + host, e);
        }
    }
}
