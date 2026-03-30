package de.evia.travelmate.trips.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantRemovedFromTrip;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;

class TripTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final UUID ORGANIZER_ID = UUID.randomUUID();
    private static final TripName NAME = new TripName("Skiurlaub 2026");
    private static final DateRange DATE_RANGE = new DateRange(
        LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)
    );

    @Test
    void planCreatesTripInPlanningStatus() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThat(trip.tripId()).isNotNull();
        assertThat(trip.tenantId()).isEqualTo(TENANT_ID);
        assertThat(trip.name()).isEqualTo(NAME);
        assertThat(trip.dateRange()).isEqualTo(DATE_RANGE);
        assertThat(trip.status()).isEqualTo(TripStatus.PLANNING);
        assertThat(trip.organizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(trip.participants()).hasSize(1);
    }

    @Test
    void planRegistersTripCreatedEvent() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThat(trip.domainEvents()).hasSize(1);
        assertThat(trip.domainEvents().getFirst()).isInstanceOf(TripCreated.class);
        final TripCreated event = (TripCreated) trip.domainEvents().getFirst();
        assertThat(event.tripId()).isEqualTo(trip.tripId().value());
        assertThat(event.tenantId()).isEqualTo(TENANT_ID.value());
    }

    @Test
    void planWithoutDateRangeCreatesPlanningContainer() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, null, ORGANIZER_ID);

        assertThat(trip.dateRange()).isNull();
        assertThat(trip.status()).isEqualTo(TripStatus.PLANNING);
    }

    @Test
    void planAddsAllProvidedParticipants() {
        final UUID member2 = UUID.randomUUID();
        final UUID dependent1 = UUID.randomUUID();

        final Trip trip = Trip.plan(
            TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID,
            List.of(ORGANIZER_ID, member2, dependent1)
        );

        assertThat(trip.participants()).hasSize(3);
        assertThat(trip.hasParticipant(ORGANIZER_ID)).isTrue();
        assertThat(trip.hasParticipant(member2)).isTrue();
        assertThat(trip.hasParticipant(dependent1)).isTrue();
    }

    @Test
    void addParticipant() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        final UUID participantId = UUID.randomUUID();

        trip.addParticipant(participantId);

        assertThat(trip.participants()).hasSize(2);
    }

    @Test
    void rejectsDuplicateParticipant() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(() -> trip.addParticipant(ORGANIZER_ID))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeParticipant() {
        final UUID participantId = UUID.randomUUID();
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID, List.of(ORGANIZER_ID, participantId));
        trip.clearDomainEvents();

        trip.removeParticipant(participantId);

        assertThat(trip.hasParticipant(participantId)).isFalse();
        assertThat(trip.participants()).hasSize(1);
        assertThat(trip.domainEvents()).singleElement().isInstanceOf(ParticipantRemovedFromTrip.class);
    }

    @Test
    void removeParticipantRejectsUnknownParticipant() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(() -> trip.removeParticipant(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void removeParticipantRejectsOrganizer() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(() -> trip.removeParticipant(ORGANIZER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("organizer");
    }

    @Test
    void grantOrganizerRightsAddsAdditionalOrganizer() {
        final UUID participantId = UUID.randomUUID();
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID, List.of(ORGANIZER_ID, participantId));

        trip.grantOrganizerRights(participantId);

        assertThat(trip.isOrganizer(participantId)).isTrue();
        assertThat(trip.organizerIds()).containsExactlyInAnyOrder(ORGANIZER_ID, participantId);
    }

    @Test
    void grantOrganizerRightsIsIdempotent() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        trip.grantOrganizerRights(ORGANIZER_ID);

        assertThat(trip.organizerIds()).containsExactly(ORGANIZER_ID);
    }

    @Test
    void grantOrganizerRightsRejectsUnknownParticipant() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(() -> trip.grantOrganizerRights(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void confirmTransitionsFromPlanning() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        trip.confirm();

        assertThat(trip.status()).isEqualTo(TripStatus.CONFIRMED);
    }

    @Test
    void startTransitionsFromConfirmed() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.confirm();

        trip.start();

        assertThat(trip.status()).isEqualTo(TripStatus.IN_PROGRESS);
    }

    @Test
    void completeTransitionsFromInProgress() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.confirm();
        trip.start();

        trip.complete();

        assertThat(trip.status()).isEqualTo(TripStatus.COMPLETED);
    }

    @Test
    void cancelFromPlanning() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        trip.cancel();

        assertThat(trip.status()).isEqualTo(TripStatus.CANCELLED);
    }

    @Test
    void cannotConfirmWhenNotPlanning() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.confirm();

        assertThatThrownBy(trip::confirm)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotConfirmWithoutDateRange() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, null, ORGANIZER_ID);

        assertThatThrownBy(trip::confirm)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("final date range");
    }

    @Test
    void cannotStartWhenNotConfirmed() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(trip::start)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeRegistersTripCompletedEvent() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.clearDomainEvents();
        trip.confirm();
        trip.start();

        trip.complete();

        assertThat(trip.domainEvents()).hasSize(1);
        assertThat(trip.domainEvents().getFirst()).isInstanceOf(TripCompleted.class);
    }

    @Test
    void setParticipantStayPeriodWithinDateRange() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.clearDomainEvents();
        final StayPeriod stayPeriod = new StayPeriod(
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        );

        trip.setParticipantStayPeriod(ORGANIZER_ID, stayPeriod);

        assertThat(trip.participants().getFirst().stayPeriod()).isEqualTo(stayPeriod);
        assertThat(trip.domainEvents()).hasSize(1);
        assertThat(trip.domainEvents().getFirst()).isInstanceOf(StayPeriodUpdated.class);
        final StayPeriodUpdated event = (StayPeriodUpdated) trip.domainEvents().getFirst();
        assertThat(event.participantId()).isEqualTo(ORGANIZER_ID);
        assertThat(event.arrivalDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        assertThat(event.departureDate()).isEqualTo(LocalDate.of(2026, 3, 20));
    }

    @Test
    void setParticipantStayPeriodRejectsOutsideDateRange() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        final StayPeriod stayPeriod = new StayPeriod(
            LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 25)
        );

        assertThatThrownBy(() -> trip.setParticipantStayPeriod(ORGANIZER_ID, stayPeriod))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setParticipantStayPeriodRejectsUnknownParticipant() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        final StayPeriod stayPeriod = new StayPeriod(
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        );

        assertThatThrownBy(() -> trip.setParticipantStayPeriod(UUID.randomUUID(), stayPeriod))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- updateDateRange ---

    @Test
    void updateDateRangeInPlanningSucceeds() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        final DateRange newRange = new DateRange(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));

        trip.updateDateRange(newRange);

        assertThat(trip.dateRange()).isEqualTo(newRange);
    }

    @Test
    void updateDateRangeResetsInvalidStayPeriods() {
        final UUID participant2 = UUID.randomUUID();
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID,
            List.of(ORGANIZER_ID, participant2));
        trip.setParticipantStayPeriod(ORGANIZER_ID,
            new StayPeriod(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)));
        trip.setParticipantStayPeriod(participant2,
            new StayPeriod(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)));
        trip.clearDomainEvents();

        final DateRange newRange = new DateRange(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));
        trip.updateDateRange(newRange);

        assertThat(trip.participants().get(0).stayPeriod()).isNull();
        assertThat(trip.participants().get(1).stayPeriod()).isNull();
    }

    @Test
    void updateDateRangeKeepsValidStayPeriods() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        final StayPeriod stayPeriod = new StayPeriod(
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20));
        trip.setParticipantStayPeriod(ORGANIZER_ID, stayPeriod);

        final DateRange widerRange = new DateRange(
            LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 25));
        trip.updateDateRange(widerRange);

        assertThat(trip.participants().getFirst().stayPeriod()).isEqualTo(stayPeriod);
    }

    @Test
    void updateDateRangeRejectsNonPlanningStatus() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);
        trip.confirm();
        final DateRange newRange = new DateRange(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));

        assertThatThrownBy(() -> trip.updateDateRange(newRange))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PLANNING");
    }

    @Test
    void updateDateRangeRejectsNull() {
        final Trip trip = Trip.plan(TENANT_ID, NAME, null, DATE_RANGE, ORGANIZER_ID);

        assertThatThrownBy(() -> trip.updateDateRange(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
