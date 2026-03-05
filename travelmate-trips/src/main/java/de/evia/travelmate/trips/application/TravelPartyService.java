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
public class TravelPartyService {

    private final TravelPartyRepository repository;

    public TravelPartyService(final TravelPartyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void onTenantCreated(final TenantCreated event) {
        final TenantId tenantId = new TenantId(event.tenantId());
        final TravelParty party = repository.findByTenantId(tenantId)
            .map(existing -> {
                existing.updateName(event.tenantName());
                return existing;
            })
            .orElseGet(() -> TravelParty.create(tenantId, event.tenantName()));
        repository.save(party);
    }

    @Transactional
    public void onAccountRegistered(final AccountRegistered event) {
        final TenantId tenantId = new TenantId(event.tenantId());
        final TravelParty party = findOrCreateParty(tenantId);
        if (!party.hasMember(event.accountId())) {
            party.addMember(event.accountId(), event.email(), event.firstName(), event.lastName());
        }
        repository.save(party);
    }

    @Transactional
    public void onDependentAdded(final DependentAddedToTenant event) {
        final TenantId tenantId = new TenantId(event.tenantId());
        final TravelParty party = findOrCreateParty(tenantId);
        party.addDependent(event.dependentId(), event.guardianAccountId(),
            event.firstName(), event.lastName());
        repository.save(party);
    }

    private TravelParty findOrCreateParty(final TenantId tenantId) {
        return repository.findByTenantId(tenantId)
            .orElseGet(() -> TravelParty.create(tenantId, tenantId.value().toString()));
    }
}
