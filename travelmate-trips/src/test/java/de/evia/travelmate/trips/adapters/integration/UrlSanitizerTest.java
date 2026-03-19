package de.evia.travelmate.trips.adapters.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UrlSanitizerTest {

    @Test
    void acceptsValidHttpsUrl() {
        final String result = UrlSanitizer.validate("https://www.huetten.com/chalet-am-kogl");

        assertThat(result).isEqualTo("https://www.huetten.com/chalet-am-kogl");
    }

    @Test
    void acceptsHttpUrl() {
        final String result = UrlSanitizer.validate("http://www.huetten.com/chalet-am-kogl");

        assertThat(result).isEqualTo("http://www.huetten.com/chalet-am-kogl");
    }

    @Test
    void rejectsNullUrl() {
        assertThatThrownBy(() -> UrlSanitizer.validate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL");
    }

    @Test
    void rejectsBlankUrl() {
        assertThatThrownBy(() -> UrlSanitizer.validate("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL");
    }

    @Test
    void rejectsFileScheme() {
        assertThatThrownBy(() -> UrlSanitizer.validate("file:///etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTTP");
    }

    @Test
    void rejectsFtpScheme() {
        assertThatThrownBy(() -> UrlSanitizer.validate("ftp://example.com/file"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTTP");
    }

    @Test
    void rejectsLocalhostUrl() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://localhost/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsLoopbackIp() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://127.0.0.1/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsPrivateIp10() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://10.0.0.1/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsPrivateIp192168() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://192.168.1.1/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsPrivateIp172() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://172.16.0.1/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsLinkLocalIp() {
        assertThatThrownBy(() -> UrlSanitizer.validate("https://169.254.1.1/admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not reachable");
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> UrlSanitizer.validate("not-a-url"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
