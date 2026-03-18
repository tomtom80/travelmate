package de.evia.travelmate.trips.domain.accommodation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RoomAssignmentTest {

    private static final RoomId ROOM_ID = new RoomId(UUID.randomUUID());
    private static final UUID PARTY_TENANT_ID = UUID.randomUUID();

    @Test
    void createAssignment() {
        final RoomAssignment assignment = RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "Familie Schmidt", 3);

        assertThat(assignment.assignmentId()).isNotNull();
        assertThat(assignment.roomId()).isEqualTo(ROOM_ID);
        assertThat(assignment.partyTenantId()).isEqualTo(PARTY_TENANT_ID);
        assertThat(assignment.partyName()).isEqualTo("Familie Schmidt");
        assertThat(assignment.personCount()).isEqualTo(3);
        assertThat(assignment.assignedAt()).isNotNull();
    }

    @Test
    void rejectsNullRoomId() {
        assertThatThrownBy(() -> RoomAssignment.create(null, PARTY_TENANT_ID, "Test", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("roomId");
    }

    @Test
    void rejectsNullPartyTenantId() {
        assertThatThrownBy(() -> RoomAssignment.create(ROOM_ID, null, "Test", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partyTenantId");
    }

    @Test
    void rejectsBlankPartyName() {
        assertThatThrownBy(() -> RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partyName");
    }

    @Test
    void rejectsZeroPersonCount() {
        assertThatThrownBy(() -> RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "Test", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Person count must be at least 1");
    }

    @Test
    void rejectsNegativePersonCount() {
        assertThatThrownBy(() -> RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "Test", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Person count must be at least 1");
    }

    @Test
    void updatePersonCount() {
        final RoomAssignment assignment = RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "Test", 2);

        assignment.updatePersonCount(4);

        assertThat(assignment.personCount()).isEqualTo(4);
    }

    @Test
    void updatePersonCountRejectsZero() {
        final RoomAssignment assignment = RoomAssignment.create(ROOM_ID, PARTY_TENANT_ID, "Test", 2);

        assertThatThrownBy(() -> assignment.updatePersonCount(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Person count must be at least 1");
    }
}
