package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@ExtendWith(MockitoExtension.class)
class TravelPartyServiceTest {

    @Mock
    private TravelPartyRepository repository;

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private TravelPartyService service;

    @Test
    void onTenantCreatedCreatesTravelParty() {
        final UUID tenantId = UUID.randomUUID();
        final TenantCreated event = new TenantCreated(tenantId, "Hüttenurlaub 2026", LocalDate.now());
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.empty());
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        service.onTenantCreated(event);

        final var captor = ArgumentCaptor.forClass(TravelParty.class);
        verify(repository).save(captor.capture());
        final TravelParty party = captor.getValue();
        assertThat(party.tenantId()).isEqualTo(new TenantId(tenantId));
        assertThat(party.name()).isEqualTo("Hüttenurlaub 2026");
    }

    @Test
    void onTenantCreatedUpdatesNameIfPartyAlreadyExists() {
        final UUID tenantId = UUID.randomUUID();
        final TravelParty existingParty = TravelParty.create(new TenantId(tenantId), tenantId.toString());
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.of(existingParty));
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        final TenantCreated event = new TenantCreated(tenantId, "Hüttenurlaub 2026", LocalDate.now());

        service.onTenantCreated(event);

        assertThat(existingParty.name()).isEqualTo("Hüttenurlaub 2026");
        verify(repository).save(existingParty);
    }

    @Test
    void onAccountRegisteredAddsMember() {
        final UUID tenantId = UUID.randomUUID();
        final UUID accountId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(tenantId), "Hüttenurlaub 2026");
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.of(party));
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        final AccountRegistered event = new AccountRegistered(
            tenantId, accountId, "max@example.com", "Max", "Mustermann",
            "max@example.com", LocalDate.now()
        );

        service.onAccountRegistered(event);

        assertThat(party.members()).hasSize(1);
        assertThat(party.members().getFirst().memberId()).isEqualTo(accountId);
        verify(repository).save(party);
    }

    @Test
    void onAccountRegisteredCreatesPartyIfNotExists() {
        final UUID tenantId = UUID.randomUUID();
        final UUID accountId = UUID.randomUUID();
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.empty());
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        final AccountRegistered event = new AccountRegistered(
            tenantId, accountId, "max@example.com", "Max", "Mustermann",
            "max@example.com", LocalDate.now()
        );

        service.onAccountRegistered(event);

        final var captor = ArgumentCaptor.forClass(TravelParty.class);
        verify(repository).save(captor.capture());
        final TravelParty savedParty = captor.getValue();
        assertThat(savedParty.tenantId()).isEqualTo(new TenantId(tenantId));
        assertThat(savedParty.members()).hasSize(1);
        assertThat(savedParty.members().getFirst().memberId()).isEqualTo(accountId);
    }

    @Test
    void onAccountRegisteredLinksAwaitingInvitations() {
        final UUID tenantId = UUID.randomUUID();
        final UUID accountId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(tenantId), "Hüttenurlaub 2026");
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.of(party));
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        final AccountRegistered event = new AccountRegistered(
            tenantId, accountId, "invited@example.com", "Invited", "User",
            "invited@example.com", LocalDate.now()
        );

        service.onAccountRegistered(event);

        verify(invitationService).linkAwaitingInvitations("invited@example.com", accountId,
            "Invited", "User");
    }

    @Test
    void onDependentAddedAddsDependentToParty() {
        final UUID tenantId = UUID.randomUUID();
        final UUID guardianId = UUID.randomUUID();
        final UUID dependentId = UUID.randomUUID();
        final TravelParty party = TravelParty.create(new TenantId(tenantId), "Hüttenurlaub 2026");
        party.addMember(guardianId, "max@example.com", "Max", "Mustermann");
        when(repository.findByTenantId(new TenantId(tenantId))).thenReturn(Optional.of(party));
        when(repository.save(any(TravelParty.class))).thenAnswer(inv -> inv.getArgument(0));

        final DependentAddedToTenant event = new DependentAddedToTenant(
            tenantId, dependentId, guardianId, "Lena", "Mustermann", LocalDate.now()
        );

        service.onDependentAdded(event);

        assertThat(party.dependents()).hasSize(1);
        assertThat(party.dependents().getFirst().dependentId()).isEqualTo(dependentId);
        verify(repository).save(party);
    }
}
