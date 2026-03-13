package de.evia.travelmate.iam.application;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.command.CreateTenantCommand;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;
import de.evia.travelmate.iam.domain.tenant.Description;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AccountRepository accountRepository;
    private final DependentRepository dependentRepository;
    private final IdentityProviderService identityProviderService;
    private final ApplicationEventPublisher eventPublisher;

    public TenantService(final TenantRepository tenantRepository,
                         final AccountRepository accountRepository,
                         final DependentRepository dependentRepository,
                         final IdentityProviderService identityProviderService,
                         final ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.accountRepository = accountRepository;
        this.dependentRepository = dependentRepository;
        this.identityProviderService = identityProviderService;
        this.eventPublisher = eventPublisher;
    }

    public TenantRepresentation createTenant(final CreateTenantCommand command) {
        final TenantName name = new TenantName(command.name());
        if (tenantRepository.existsByName(name)) {
            throw new DuplicateEntityException("signup.error.tenantExists");
        }
        final Description description = command.description() != null
            ? new Description(command.description())
            : null;
        final Tenant tenant = Tenant.create(name, description);
        final Tenant saved = tenantRepository.save(tenant);
        return new TenantRepresentation(saved);
    }

    @Transactional(readOnly = true)
    public TenantRepresentation findById(final TenantId tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId.value().toString()));
        return new TenantRepresentation(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantRepresentation> findAll() {
        return tenantRepository.findAll().stream()
            .map(TenantRepresentation::new)
            .toList();
    }

    public void deleteTenant(final TenantId tenantId) {
        final Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId.value().toString()));
        tenant.markForDeletion();

        final List<Account> accounts = accountRepository.findAllByTenantId(tenantId);
        for (final Account account : accounts) {
            try {
                identityProviderService.deleteUser(account.keycloakUserId());
            } catch (final Exception ignored) {
            }
        }
        dependentRepository.deleteAllByTenantId(tenantId);
        accountRepository.deleteAllByTenantId(tenantId);
        tenantRepository.deleteById(tenantId);

        for (final DomainEvent event : tenant.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        tenant.clearDomainEvents();
    }
}
