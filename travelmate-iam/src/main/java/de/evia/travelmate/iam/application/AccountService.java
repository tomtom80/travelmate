package de.evia.travelmate.iam.application;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.DateOfBirth;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.dependent.DependentId;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;
import de.evia.travelmate.iam.domain.registration.InvitationToken;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final DependentRepository dependentRepository;
    private final IdentityProviderService identityProviderService;
    private final RegistrationService registrationService;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(final AccountRepository accountRepository,
                          final DependentRepository dependentRepository,
                          final IdentityProviderService identityProviderService,
                          final RegistrationService registrationService,
                          final ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.dependentRepository = dependentRepository;
        this.identityProviderService = identityProviderService;
        this.registrationService = registrationService;
        this.eventPublisher = eventPublisher;
    }

    public AccountRepresentation registerAccount(final RegisterAccountCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final Username username = new Username(command.username());
        if (accountRepository.existsByUsername(tenantId, username)) {
            throw new DuplicateEntityException("member.error.alreadyExists");
        }
        final Account account = Account.register(
            tenantId,
            new KeycloakUserId(command.keycloakUserId()),
            username,
            new Email(command.email()),
            new FullName(command.firstName(), command.lastName()),
            new DateOfBirth(command.dateOfBirth())
        );
        final Account saved = accountRepository.save(account);
        publishEvents(saved);
        return new AccountRepresentation(saved);
    }

    public InviteMemberResult inviteMember(final InviteMemberCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final Email email = new Email(command.email());
        final FullName fullName = new FullName(command.firstName(), command.lastName());

        if (accountRepository.existsByUsername(tenantId, new Username(email.value()))) {
            throw new DuplicateEntityException("member.error.alreadyExists");
        }

        final KeycloakUserId keycloakUserId = identityProviderService.createInvitedUser(email, fullName);

        try {
            final Account account = Account.register(
                tenantId,
                keycloakUserId,
                new Username(email.value()),
                email,
                fullName,
                new DateOfBirth(command.dateOfBirth())
            );
            final Account saved = accountRepository.save(account);
            identityProviderService.assignRole(keycloakUserId, "organizer");
            final InvitationToken token = registrationService.generateToken(saved.accountId());
            publishEvents(saved);
            return new InviteMemberResult(new AccountRepresentation(saved), token.tokenValue());
        } catch (final Exception e) {
            identityProviderService.deleteUser(keycloakUserId);
            throw e;
        }
    }

    public DependentRepresentation addDependent(final AddDependentCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final AccountId guardianId = new AccountId(command.guardianAccountId());
        accountRepository.findById(guardianId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Account", command.guardianAccountId().toString()));
        final Dependent dependent = Dependent.add(
            tenantId,
            guardianId,
            new FullName(command.firstName(), command.lastName()),
            new DateOfBirth(command.dateOfBirth())
        );
        final Dependent saved = dependentRepository.save(dependent);
        publishEvents(saved);
        return new DependentRepresentation(saved);
    }

    public void deleteDependent(final UUID dependentId) {
        final Dependent dependent = dependentRepository.findById(new DependentId(dependentId))
            .orElseThrow(() -> new EntityNotFoundException("Dependent", dependentId.toString()));
        dependent.markForRemoval();
        publishEvents(dependent);
        dependentRepository.deleteById(new DependentId(dependentId));
    }

    public void deleteMember(final AccountId accountId, final TenantId tenantId) {
        final long memberCount = accountRepository.countByTenantId(tenantId);
        if (memberCount <= 1) {
            throw new BusinessRuleViolationException("member.error.lastMember");
        }
        final Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account", accountId.value().toString()));
        account.markForRemoval();
        publishEvents(account);
        try {
            identityProviderService.deleteUser(account.keycloakUserId());
        } catch (final Exception ignored) {
        }
        accountRepository.deleteById(accountId);
    }

    @Transactional(readOnly = true)
    public AccountRepresentation findById(final AccountId accountId) {
        final Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account", accountId.value().toString()));
        return new AccountRepresentation(account);
    }

    @Transactional(readOnly = true)
    public List<AccountRepresentation> findAllByTenantId(final TenantId tenantId) {
        return accountRepository.findAllByTenantId(tenantId).stream()
            .map(AccountRepresentation::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DependentRepresentation> findDependentsByGuardian(final AccountId guardianAccountId) {
        return dependentRepository.findAllByGuardian(guardianAccountId).stream()
            .map(DependentRepresentation::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DependentRepresentation> findDependentsByTenantId(final TenantId tenantId) {
        return dependentRepository.findAllByTenantId(tenantId).stream()
            .map(DependentRepresentation::new)
            .toList();
    }

    private void publishEvents(final Account account) {
        for (final DomainEvent event : account.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        account.clearDomainEvents();
    }

    private void publishEvents(final Dependent dependent) {
        for (final DomainEvent event : dependent.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        dependent.clearDomainEvents();
    }
}
