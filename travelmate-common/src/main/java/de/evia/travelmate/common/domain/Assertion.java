package de.evia.travelmate.common.domain;

import java.util.Objects;

public final class Assertion {

    private Assertion() {
    }

    public static void argumentIsNotNull(final Object parameter, final String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException(
                String.format("The %s is required and must be provided.", parameterName));
        }
    }

    public static void argumentIsNotBlank(final String parameter, final String parameterName) {
        if (parameter == null || parameter.trim().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("The %s must not be null, empty or a whitespace sequence.", parameterName));
        }
    }

    public static void argumentEquals(final Object expected, final Object actual, final String errorMessage) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void argumentIsTrue(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
