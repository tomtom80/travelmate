package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ParticipantWeightingTest {

    @Test
    void createsValidWeighting() {
        final UUID participantId = UUID.randomUUID();
        final ParticipantWeighting weighting = new ParticipantWeighting(participantId, BigDecimal.ONE);

        assertThat(weighting.participantId()).isEqualTo(participantId);
        assertThat(weighting.weight()).isEqualByComparingTo("1");
    }

    @Test
    void allowsZeroWeight() {
        final ParticipantWeighting weighting = new ParticipantWeighting(UUID.randomUUID(), BigDecimal.ZERO);

        assertThat(weighting.weight()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new ParticipantWeighting(UUID.randomUUID(), new BigDecimal("-0.5")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative");
    }

    @Test
    void rejectsNullParticipantId() {
        assertThatThrownBy(() -> new ParticipantWeighting(null, BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullWeight() {
        assertThatThrownBy(() -> new ParticipantWeighting(UUID.randomUUID(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
