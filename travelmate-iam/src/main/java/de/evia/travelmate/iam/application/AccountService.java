package de.evia.travelmate.iam.application;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
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

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final DependentRepository dependentRepository;
    private final IdentityProviderService identityProviderService;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(final AccountRepository accountRepository,
                          final DependentRepository dependentRepository,
                          final IdentityProviderService identityProviderService,
                          final ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.dependentRepository = dependentRepository;
        this.identityProviderService = identityProviderService;
        this.eventPublisher = eventPublisher;
    }

    public AccountRepresentation registerAccount(final RegisterAccountCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final Username username = new Username(command.username());
        if (accountRepository.existsByUsername(tenantId, username)) {
            throw new IllegalArgumentException("Username '" + command.username() + "' is already taken.");
        }
        final Account account = Account.register(
            tenantId,
            new KeycloakUserId(command.keycloakUserId()),
            username,
            new Email(command.email()),
            new FullName(command.firstName(), command.lastName())
        );
        final Account saved = accountRepository.save(account);
        publishEvents(saved);
        return new AccountRepresentation(saved);
    }

    public AccountRepresentation inviteMember(final InviteMemberCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final Email email = new Email(command.email());
        final FullName fullName = new FullName(command.firstName(), command.lastName());

        if (accountRepository.existsByUsername(tenantId, new Username(email.value()))) {
            throw new IllegalArgumentException("member.error.alreadyExists");
        }

        final KeycloakUserId keycloakUserId = identityProviderService.createInvitedUser(email, fullName);

        try {
            final DateOfBirth dateOfBirth = command.dateOfBirth() != null
                ? new DateOfBirth(command.dateOfBirth()) : null;
            final Account account = Account.register(
                tenantId,
                keycloakUserId,
                new Username(email.value()),
                email,
                fullName,
                dateOfBirth
            );
            final Account saved = accountRepository.save(account);
            identityProviderService.assignRole(keycloakUserId, "organizer");
            identityProviderService.sendActionsEmail(keycloakUserId);
            publishEvents(saved);
            return new AccountRepresentation(saved);
        } catch (final Exception e) {
            identityProviderService.deleteUser(keycloakUserId);
            throw e;
        }
    }

    public DependentRepresentation addDependent(final AddDependentCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final AccountId guardianId = new AccountId(command.guardianAccountId());
        accountRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Guardian account not found: " + command.guardianAccountId()));
        final DateOfBirth dateOfBirth = command.dateOfBirth() != null
            ? new DateOfBirth(command.dateOfBirth()) : null;
        final Dependent dependent = Dependent.add(
            tenantId,
            guardianId,
            new FullName(command.firstName(), command.lastName()),
            dateOfBirth
        );
        final Dependent saved = dependentRepository.save(dependent);
        publishEvents(saved);
        return new DependentRepresentation(saved);
    }

    public void deleteDependent(final UUID dependentId) {
        final Dependent dependent = dependentRepository.findById(new DependentId(dependentId))
            .orElseThrow(() -> new IllegalArgumentException("Dependent not found: " + dependentId));
        dependent.markForRemoval();
        publishEvents(dependent);
        dependentRepository.deleteById(new DependentId(dependentId));
    }

    public void deleteMember(final AccountId accountId, final TenantId tenantId) {
        final long memberCount = accountRepository.countByTenantId(tenantId);
        if (memberCount <= 1) {
            throw new IllegalArgumentException("member.error.lastMember");
        }
        final Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId.value()));
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
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId.value()));
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
