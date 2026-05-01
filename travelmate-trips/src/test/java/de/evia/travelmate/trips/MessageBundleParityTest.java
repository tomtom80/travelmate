package de.evia.travelmate.trips;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageBundleParityTest {

    @ParameterizedTest(name = "bundle parity: {0}")
    @MethodSource("bundlePaths")
    void key_sets_must_be_identical(final String pair,
                                    final String leftResource,
                                    final String rightResource) throws Exception {
        final Set<String> left = loadKeys(leftResource);
        final Set<String> right = loadKeys(rightResource);

        final Set<String> onlyInLeft = new TreeSet<>(left);
        onlyInLeft.removeAll(right);
        final Set<String> onlyInRight = new TreeSet<>(right);
        onlyInRight.removeAll(left);

        assertThat(onlyInLeft)
            .as("Keys in %s but missing from %s", leftResource, rightResource)
            .isEmpty();
        assertThat(onlyInRight)
            .as("Keys in %s but missing from %s", rightResource, leftResource)
            .isEmpty();
    }

    static Stream<Arguments> bundlePaths() {
        return Stream.of(
            Arguments.of("trips-de-vs-en", "/messages_de.properties", "/messages_en.properties"),
            Arguments.of("trips-default-vs-en", "/messages.properties", "/messages_en.properties")
        );
    }

    private static Set<String> loadKeys(final String resourcePath) throws Exception {
        final Properties props = new Properties();
        try (final var stream = MessageBundleParityTest.class.getResourceAsStream(resourcePath)) {
            assertThat(stream).as("classpath resource not found: %s", resourcePath).isNotNull();
            props.load(stream);
        }
        return props.stringPropertyNames();
    }
}
