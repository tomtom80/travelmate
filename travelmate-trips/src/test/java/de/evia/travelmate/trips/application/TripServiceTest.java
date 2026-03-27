package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.mockito.ArgumentCaptor;

import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.AddParticipantToTripCommand;
import de.evia.travelmate.trips.application.command.GrantTripOrganizerCommand;
import de.evia.travelmate.trips.application.command.RemoveParticipantFromTripCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemId;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingList;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListRepository;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID ORGANIZER_ID = UUID.randomUUID();
    private static final UUID PARTY_MEMBER_ID = UUID.randomUUID();
    private static final UUID OTHER_PARTY_PARTICIPANT_ID = UUID.randomUUID();

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelPartyRepository travelPartyRepository;

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TripParticipationEventPublisher tripParticipationEventPublisher;

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
    void createTripAddsAllTravelPartyMembersAndDependents() {
        final UUID member2Id = UUID.randomUUID();
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(member2Id, "lisa@example.com", "Lisa", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Sommerurlaub", null,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
            ORGANIZER_ID
        );

        final TripRepresentation result = tripService.createTrip(command);

        final ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        final Trip savedTrip = tripCaptor.getValue();
        assertThat(savedTrip.participants()).hasSize(3);
        assertThat(savedTrip.hasParticipant(ORGANIZER_ID)).isTrue();
        assertThat(savedTrip.hasParticipant(member2Id)).isTrue();
        assertThat(savedTrip.hasParticipant(dependentId)).isTrue();
    }

    @Test
    void createTripPublishesParticipantJoinedEventsForAllMembers() {
        final UUID member2Id = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann", LocalDate.of(1985, 5, 1));
        party.addMember(member2Id, "lisa@example.com", "Lisa", "Mustermann", LocalDate.of(1988, 7, 2));
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(Optional.of(party));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateTripCommand command = new CreateTripCommand(
            TENANT_UUID, "Wanderung", null,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7),
            ORGANIZER_ID
        );

        tripService.createTrip(command);

        final ArgumentCaptor<ParticipantJoinedTrip> eventCaptor = ArgumentCaptor.forClass(ParticipantJoinedTrip.class);
        verify(tripParticipationEventPublisher, atLeastOnce()).publishParticipantJoinedAfterCommit(eventCaptor.capture());
        final List<ParticipantJoinedTrip> joinedEvents = eventCaptor.getAllValues();
        assertThat(joinedEvents).hasSize(2);
        assertThat(joinedEvents).extracting(ParticipantJoinedTrip::participantId)
            .containsExactlyInAnyOrder(ORGANIZER_ID, member2Id);
        assertThat(joinedEvents).allSatisfy(event -> assertThat(event.accountHolder()).isTrue());
        assertThat(joinedEvents).extracting(ParticipantJoinedTrip::dateOfBirth)
            .containsExactlyInAnyOrder(LocalDate.of(1985, 5, 1), LocalDate.of(1988, 7, 2));
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

    @Test
    void confirmTripTransitionsToConfirmed() {
        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.confirmTrip(trip.tripId());

        assertThat(trip.status().name()).isEqualTo("CONFIRMED");
        verify(tripRepository).save(trip);
    }

    @Test
    void completeTripPublishesTripCompletedEvent() {
        final Trip trip = createTrip();
        trip.clearDomainEvents();
        trip.confirm();
        trip.start();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.completeTrip(trip.tripId());

        assertThat(trip.status().name()).isEqualTo("COMPLETED");
        verify(eventPublisher).publishEvent(any(TripCompleted.class));
    }

    @Test
    void setStayPeriodUpdatesParticipant() {
        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        tripService.setStayPeriod(new SetStayPeriodCommand(
            trip.tripId().value(), ORGANIZER_ID,
            ORGANIZER_ID, TENANT_UUID,
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        ));

        assertThat(trip.participants().getFirst().stayPeriod()).isNotNull();
        verify(tripRepository).save(trip);
    }

    @Test
    void setStayPeriodAllowsPartyMemberToUpdateOwnPartyParticipant() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(PARTY_MEMBER_ID, "lisa@example.com", "Lisa", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = Trip.planWithParticipants(
            new TenantId(TENANT_UUID),
            new TripName("Skiurlaub"),
            null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID,
            List.of(
                new de.evia.travelmate.trips.domain.trip.Participant(ORGANIZER_ID, "Max", "Mustermann"),
                new de.evia.travelmate.trips.domain.trip.Participant(PARTY_MEMBER_ID, "Lisa", "Mustermann")
            )
        );
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.setStayPeriod(new SetStayPeriodCommand(
            trip.tripId().value(), ORGANIZER_ID,
            PARTY_MEMBER_ID, TENANT_UUID,
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        ));

        assertThat(trip.participants().getFirst().stayPeriod()).isNotNull();
    }

    @Test
    void setStayPeriodRejectsPartyMemberForParticipantOutsideOwnParty() {
        final TravelParty ownParty = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        ownParty.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        ownParty.addMember(PARTY_MEMBER_ID, "lisa@example.com", "Lisa", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(ownParty));

        final Trip trip = Trip.planWithParticipants(
            new TenantId(TENANT_UUID),
            new TripName("Skiurlaub"),
            null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID,
            List.of(
                new de.evia.travelmate.trips.domain.trip.Participant(ORGANIZER_ID, "Max", "Mustermann"),
                new de.evia.travelmate.trips.domain.trip.Participant(OTHER_PARTY_PARTICIPANT_ID, "Tom", "Anders")
            )
        );
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.setStayPeriod(new SetStayPeriodCommand(
            trip.tripId().value(), OTHER_PARTY_PARTICIPANT_ID,
            PARTY_MEMBER_ID, TENANT_UUID,
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("own travel party");
    }

    @Test
    void grantTripOrganizerAllowsExistingOrganizerToPromoteParticipant() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(PARTY_MEMBER_ID, "lisa@example.com", "Lisa", "Mustermann");
        when(travelPartyRepository.findAll()).thenReturn(List.of(party));
        final Trip trip = Trip.planWithParticipants(
            new TenantId(TENANT_UUID),
            new TripName("Skiurlaub"),
            null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID,
            List.of(
                new de.evia.travelmate.trips.domain.trip.Participant(ORGANIZER_ID, "Max", "Mustermann"),
                new de.evia.travelmate.trips.domain.trip.Participant(PARTY_MEMBER_ID, "Lisa", "Mustermann")
            )
        );
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.grantTripOrganizer(new GrantTripOrganizerCommand(
            trip.tripId().value(), PARTY_MEMBER_ID, ORGANIZER_ID
        ));

        assertThat(trip.isOrganizer(PARTY_MEMBER_ID)).isTrue();
        verify(tripRepository).save(trip);
    }

    @Test
    void grantTripOrganizerRejectsNonOrganizerActor() {
        final Trip trip = Trip.planWithParticipants(
            new TenantId(TENANT_UUID),
            new TripName("Skiurlaub"),
            null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID,
            List.of(
                new de.evia.travelmate.trips.domain.trip.Participant(ORGANIZER_ID, "Max", "Mustermann"),
                new de.evia.travelmate.trips.domain.trip.Participant(PARTY_MEMBER_ID, "Lisa", "Mustermann")
            )
        );
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.grantTripOrganizer(new GrantTripOrganizerCommand(
            trip.tripId().value(), ORGANIZER_ID, PARTY_MEMBER_ID
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("existing trip organizer");
    }

    @Test
    void grantTripOrganizerRejectsParticipantWithoutAccount() {
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann");
        when(travelPartyRepository.findAll()).thenReturn(List.of(party));

        final Trip trip = Trip.planWithParticipants(
            new TenantId(TENANT_UUID),
            new TripName("Skiurlaub"),
            null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID,
            List.of(
                new de.evia.travelmate.trips.domain.trip.Participant(ORGANIZER_ID, "Max", "Mustermann"),
                new de.evia.travelmate.trips.domain.trip.Participant(dependentId, "Tim", "Mustermann")
            )
        );
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.grantTripOrganizer(new GrantTripOrganizerCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("with an account");
    }

    @Test
    void addParticipantToTripAllowsOwnPartyMember() {
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann", LocalDate.of(2023, 1, 15));
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.addParticipantToTrip(new AddParticipantToTripCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID, TENANT_UUID
        ));

        assertThat(trip.hasParticipant(dependentId)).isTrue();
        verify(tripRepository).save(trip);
        final ArgumentCaptor<ParticipantJoinedTrip> eventCaptor = ArgumentCaptor.forClass(ParticipantJoinedTrip.class);
        verify(tripParticipationEventPublisher, atLeastOnce()).publishParticipantJoinedAfterCommit(eventCaptor.capture());
        final ParticipantJoinedTrip joinedEvent = eventCaptor.getValue();
        assertThat(joinedEvent.accountHolder()).isFalse();
        assertThat(joinedEvent.dateOfBirth()).isEqualTo(LocalDate.of(2023, 1, 15));
    }

    @Test
    void addParticipantToTripRejectsAlreadyExistingParticipant() {
        final UUID member2Id = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addMember(member2Id, "lisa@example.com", "Lisa", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = Trip.plan(new TenantId(TENANT_UUID), new TripName("Skiurlaub"), null,
            new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID, List.of(ORGANIZER_ID, member2Id));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.addParticipantToTrip(new AddParticipantToTripCommand(
            trip.tripId().value(), member2Id, ORGANIZER_ID, TENANT_UUID
        )))
            .isInstanceOf(DuplicateEntityException.class)
            .hasMessageContaining("participant.error.alreadyExists");
    }

    @Test
    void addParticipantToTripRejectsDuplicateDependent() {
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann", LocalDate.of(2023, 1, 15));
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = createTrip();
        trip.addParticipant(dependentId, "Tim", "Mustermann");
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.addParticipantToTrip(new AddParticipantToTripCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID, TENANT_UUID
        )))
            .isInstanceOf(DuplicateEntityException.class)
            .hasMessageContaining("participant.error.alreadyExists");
    }

    @Test
    void addParticipantToTripRejectsParticipantOutsideOwnParty() {
        final UUID outsiderId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = createTrip();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.addParticipantToTrip(new AddParticipantToTripCommand(
            trip.tripId().value(), outsiderId, ORGANIZER_ID, TENANT_UUID
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("own travel party");
    }

    @Test
    void removeParticipantFromTripAllowsOwnPartyMember() {
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = Trip.plan(new TenantId(TENANT_UUID), new TripName("Skiurlaub"), null, new DateRange(
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)
        ), ORGANIZER_ID, List.of(ORGANIZER_ID, dependentId));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        tripService.removeParticipantFromTrip(new RemoveParticipantFromTripCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID, TENANT_UUID
        ));

        assertThat(trip.hasParticipant(dependentId)).isFalse();
        verify(tripRepository).save(trip);
    }

    @Test
    void removeParticipantFromTripRejectsCompletedTrip() {
        final UUID dependentId = UUID.randomUUID();
        final Trip trip = Trip.plan(new TenantId(TENANT_UUID), new TripName("Skiurlaub"), null, new DateRange(
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)
        ), ORGANIZER_ID, List.of(ORGANIZER_ID, dependentId));
        trip.confirm();
        trip.start();
        trip.complete();
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.removeParticipantFromTrip(new RemoveParticipantFromTripCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID, TENANT_UUID
        )))
            .isInstanceOf(de.evia.travelmate.common.domain.BusinessRuleViolationException.class)
            .hasMessageContaining("completedTripRemovalNotAllowed");

        verify(tripRepository, never()).save(any(Trip.class));
        verify(shoppingListRepository, never()).save(any(ShoppingList.class));
    }

    @Test
    void removeParticipantFromTripClearsOpenShoppingAssignmentsButPreservesPurchasedHistory() {
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Familie Test");
        party.addMember(ORGANIZER_ID, "max@example.com", "Max", "Mustermann");
        party.addDependent(dependentId, ORGANIZER_ID, "Tim", "Mustermann");
        when(travelPartyRepository.findByTenantId(new TenantId(TENANT_UUID))).thenReturn(Optional.of(party));

        final Trip trip = Trip.plan(new TenantId(TENANT_UUID), new TripName("Skiurlaub"), null, new DateRange(
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)
        ), ORGANIZER_ID, List.of(ORGANIZER_ID, dependentId));
        when(tripRepository.findById(trip.tripId())).thenReturn(Optional.of(trip));

        final ShoppingList shoppingList = ShoppingList.generate(trip.tenantId(), trip.tripId(), List.of());
        final ShoppingItemId openItemId = shoppingList.addManualItem("Water", BigDecimal.ONE, "bottle");
        shoppingList.assignItem(openItemId, dependentId);
        final ShoppingItemId purchasedItemId = shoppingList.addManualItem("Bread", BigDecimal.ONE, "pcs");
        shoppingList.assignItem(purchasedItemId, dependentId);
        shoppingList.markPurchased(purchasedItemId, dependentId);
        when(shoppingListRepository.findByTripIdAndTenantId(trip.tripId(), trip.tenantId()))
            .thenReturn(Optional.of(shoppingList));

        tripService.removeParticipantFromTrip(new RemoveParticipantFromTripCommand(
            trip.tripId().value(), dependentId, ORGANIZER_ID, TENANT_UUID
        ));

        final ShoppingItem openItem = shoppingList.items().stream()
            .filter(item -> item.shoppingItemId().equals(openItemId))
            .findFirst()
            .orElseThrow();
        final ShoppingItem purchasedItem = shoppingList.items().stream()
            .filter(item -> item.shoppingItemId().equals(purchasedItemId))
            .findFirst()
            .orElseThrow();

        assertThat(openItem.status()).isEqualTo(de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus.OPEN);
        assertThat(openItem.assignedTo()).isNull();
        assertThat(purchasedItem.status())
            .isEqualTo(de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus.PURCHASED);
        assertThat(purchasedItem.assignedTo()).isEqualTo(dependentId);
        verify(shoppingListRepository).save(shoppingList);
    }

    private Trip createTrip() {
        return Trip.plan(
            new TenantId(TENANT_UUID), new TripName("Skiurlaub"),
            null, new DateRange(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 22)),
            ORGANIZER_ID
        );
    }
}
