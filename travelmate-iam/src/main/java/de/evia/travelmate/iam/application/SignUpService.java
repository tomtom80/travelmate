package de.evia.travelmate.iam.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.iam.application.command.RegisterExternalUserCommand;
import de.evia.travelmate.iam.application.command.SignUpCommand;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.DateOfBirth;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
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
    private final RegistrationService registrationService;
    private final ApplicationEventPublisher eventPublisher;

    public SignUpService(final TenantRepository tenantRepository,
                         final AccountRepository accountRepository,
                         final IdentityProviderService identityProviderService,
                         final RegistrationService registrationService,
                         final ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.accountRepository = accountRepository;
        this.identityProviderService = identityProviderService;
        this.registrationService = registrationService;
        this.eventPublisher = eventPublisher;
    }

    public void signUp(final SignUpCommand command) {
        final TenantName tenantName = new TenantName(command.tenantName());
        if (tenantRepository.existsByName(tenantName)) {
            throw new DuplicateEntityException("signup.error.tenantExists");
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
                fullName,
                new DateOfBirth(command.dateOfBirth())
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

    public InviteMemberResult registerExternalUser(final RegisterExternalUserCommand command) {
        final String tenantNameStr = "Reisepartei " + command.firstName() + " " + command.lastName();
        final TenantName tenantName = new TenantName(tenantNameStr);

        final Tenant tenant = Tenant.create(tenantName, new Description(""));
        tenantRepository.save(tenant);

        for (final DomainEvent event : tenant.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        tenant.clearDomainEvents();

        final Email email = new Email(command.email());
        final FullName fullName = new FullName(command.firstName(), command.lastName());

        final KeycloakUserId keycloakUserId = identityProviderService.createInvitedUser(email, fullName);

        try {
            final Account account = Account.register(
                tenant.tenantId(),
                keycloakUserId,
                new Username(email.value()),
                email,
                fullName,
                new DateOfBirth(command.dateOfBirth())
            );
            final Account saved = accountRepository.save(account);
            identityProviderService.assignRole(keycloakUserId, "organizer");
            final InvitationToken token = registrationService.generateToken(saved.accountId());

            for (final DomainEvent event : saved.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            saved.clearDomainEvents();

            return new InviteMemberResult(
                new de.evia.travelmate.iam.application.representation.AccountRepresentation(saved),
                token.tokenValue()
            );
        } catch (final Exception e) {
            identityProviderService.deleteUser(keycloakUserId);
            throw e;
        }
    }
}
