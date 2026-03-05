package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID ORGANIZER_ID = UUID.randomUUID();

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelPartyRepository travelPartyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TripService tripService;

    @Test
    void createTripReturnsTripRepresentation() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Skiurlaub", "Ab in die Berge",
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            ORGANIZER_ID
        );

        final TripRepresentation result = tripService.createTrip(command);

        assertThat(result.name()).isEqualTo("Skiurlaub");
        assertThat(result.status()).isEqualTo("PLANNING");
        assertThat(result.organizerId()).isEqualTo(ORGANIZER_ID);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTripRejectsUnknownOrganizer() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Skiurlaub", null,
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22),
            UUID.randomUUID()
        );

        assertThatThrownBy(() -> tripService.createTrip(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a member");
    }
}
