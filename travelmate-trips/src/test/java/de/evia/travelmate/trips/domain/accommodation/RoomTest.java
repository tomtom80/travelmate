package de.evia.travelmate.trips.domain.accommodation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void createRoomWithAllFields() {
        final Room room = new Room("Zimmer 1", RoomType.DOUBLE, 2, new BigDecimal("80.00"));

        assertThat(room.roomId()).isNotNull();
        assertThat(room.name()).isEqualTo("Zimmer 1");
        assertThat(room.roomType()).isEqualTo(RoomType.DOUBLE);
        assertThat(room.bedCount()).isEqualTo(2);
        assertThat(room.pricePerNight()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void createRoomWithNullPrice() {
        final Room room = new Room("Matratzenlager", RoomType.DORMITORY, 10, null);

        assertThat(room.pricePerNight()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Room("", RoomType.SINGLE, 1, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNameExceeding100Characters() {
        final String longName = "A".repeat(101);
        assertThatThrownBy(() -> new Room(longName, RoomType.SINGLE, 1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("100 characters");
    }

    @Test
    void rejectsZeroBedCount() {
        assertThatThrownBy(() -> new Room("Zimmer", RoomType.SINGLE, 0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 1");
    }

    @Test
    void rejectsNegativeBedCount() {
        assertThatThrownBy(() -> new Room("Zimmer", RoomType.SINGLE, -1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 1");
    }

    @Test
    void rejectsNegativePricePerNight() {
        assertThatThrownBy(() -> new Room("Zimmer", RoomType.SINGLE, 1, new BigDecimal("-10.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 or positive");
    }

    @Test
    void acceptsZeroPricePerNight() {
        final Room room = new Room("Zimmer", RoomType.SINGLE, 1, BigDecimal.ZERO);
        assertThat(room.pricePerNight()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
