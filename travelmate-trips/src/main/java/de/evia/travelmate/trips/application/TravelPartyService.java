package de.evia.travelmate.trips.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Service
@Transactional
public class TravelPartyService {

    private final TravelPartyRepository repository;

    public TravelPartyService(final TravelPartyRepository repository) {
        this.repository = repository;
    }

    public void onTenantCreated(final TenantCreated event) {
        final TravelParty party = TravelParty.create(
            new TenantId(event.tenantId()),
            event.tenantName()
        );
        repository.save(party);
    }

    public void onAccountRegistered(final AccountRegistered event) {
        final TenantId tenantId = new TenantId(event.tenantId());
        final TravelParty party = repository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalStateException(
                "TravelParty not found for tenant " + event.tenantId()));
        party.addMember(event.accountId(), event.email(), event.firstName(), event.lastName());
        repository.save(party);
    }

    public void onDependentAdded(final DependentAddedToTenant event) {
        final TenantId tenantId = new TenantId(event.tenantId());
        final TravelParty party = repository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalStateException(
                "TravelParty not found for tenant " + event.tenantId()));
        party.addDependent(event.dependentId(), event.guardianAccountId(),
            event.firstName(), event.lastName());
        repository.save(party);
    }
}
