package de.evia.travelmate.iam.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.iam.application.command.SignUpCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.tenant.Description;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@Service
@Transactional
public class SignUpService {

    private final TenantRepository tenantRepository;
    private final AccountRepository accountRepository;
    private final IdentityProviderService identityProviderService;
    private final ApplicationEventPublisher eventPublisher;

    public SignUpService(final TenantRepository tenantRepository,
                         final AccountRepository accountRepository,
                         final IdentityProviderService identityProviderService,
                         final ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.accountRepository = accountRepository;
        this.identityProviderService = identityProviderService;
        this.eventPublisher = eventPublisher;
    }

    public void signUp(final SignUpCommand command) {
        final TenantName tenantName = new TenantName(command.tenantName());
        if (tenantRepository.existsByName(tenantName)) {
            throw new IllegalArgumentException(
                "A travel group with the name '" + command.tenantName() + "' already exists.");
        }

        final Tenant tenant = Tenant.create(tenantName, new Description(""));
        tenantRepository.save(tenant);

        for (final DomainEvent event : tenant.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        tenant.clearDomainEvents();

        final Email email = new Email(command.email());
        final FullName fullName = new FullName(command.firstName(), command.lastName());
        final Password password = new Password(command.password());

        final KeycloakUserId keycloakUserId = identityProviderService.createUser(email, fullName, password);

        try {
            final Account account = Account.register(
                tenant.tenantId(),
                keycloakUserId,
                new Username(email.value()),
                email,
                fullName
            );
            accountRepository.save(account);
            identityProviderService.assignRole(keycloakUserId, "organizer");

            for (final DomainEvent event : account.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            account.clearDomainEvents();
        } catch (final Exception e) {
            identityProviderService.deleteUser(keycloakUserId);
            throw e;
        }
    }
}
