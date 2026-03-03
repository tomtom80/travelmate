package de.evia.travelmate.common.domain;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class AssertionTest {

    @Test
    void argumentIsNotNull_throwsForNull() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentIsNotNull(null, "param"))
            .withMessageContaining("param");
    }

    @Test
    void argumentIsNotNull_passesForNonNull() {
        assertThatNoException()
            .isThrownBy(() -> Assertion.argumentIsNotNull("value", "param"));
    }

    @Test
    void argumentIsNotBlank_throwsForNull() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentIsNotBlank(null, "param"));
    }

    @Test
    void argumentIsNotBlank_throwsForEmpty() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentIsNotBlank("", "param"));
    }

    @Test
    void argumentIsNotBlank_throwsForWhitespace() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentIsNotBlank("   ", "param"));
    }

    @Test
    void argumentIsNotBlank_passesForNonBlank() {
        assertThatNoException()
            .isThrownBy(() -> Assertion.argumentIsNotBlank("value", "param"));
    }

    @Test
    void argumentEquals_throwsWhenNotEqual() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentEquals("a", "b", "must be equal"));
    }

    @Test
    void argumentEquals_passesWhenEqual() {
        assertThatNoException()
            .isThrownBy(() -> Assertion.argumentEquals("a", "a", "must be equal"));
    }

    @Test
    void argumentIsTrue_throwsWhenFalse() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertion.argumentIsTrue(false, "must be true"));
    }

    @Test
    void argumentIsTrue_passesWhenTrue() {
        assertThatNoException()
            .isThrownBy(() -> Assertion.argumentIsTrue(true, "must be true"));
    }
}
