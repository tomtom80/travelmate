package de.evia.travelmate.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.DependentRemovedFromTenant;
import de.evia.travelmate.common.events.iam.MemberRemovedFromTenant;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.dependent.DependentId;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
import de.evia.travelmate.iam.domain.tripparticipation.TripParticipationRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DependentRepository dependentRepository;

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private TripParticipationRepository tripParticipationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 5, 15);

    @Test
    void registersAccount() {
        final RegisterAccountCommand command = new RegisterAccountCommand(
            IamTestFixtures.TENANT_ID.value(), "kc-123", "testuser",
            "test@example.com", "Max", "Mustermann", DATE_OF_BIRTH
        );
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final AccountRepresentation result = accountService.registerAccount(command);

        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.firstName()).isEqualTo("Max");
        verify(accountRepository).save(any(Account.class));
        verify(eventPublisher).publishEvent(any(AccountRegistered.class));
    }

    @Test
    void rejectsDuplicateUsername() {
        final RegisterAccountCommand command = new RegisterAccountCommand(
            IamTestFixtures.TENANT_ID.value(), "kc-123", "testuser",
            "test@example.com", "Max", "Mustermann", DATE_OF_BIRTH
        );
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(true);

        assertThatThrownBy(() -> accountService.registerAccount(command))
            .isInstanceOf(DuplicateEntityException.class)
            .hasMessageContaining("alreadyExists");
    }

    @Test
    void addsDependentToAccount() {
        final AddDependentCommand command = new AddDependentCommand(
            IamTestFixtures.TENANT_ID.value(), IamTestFixtures.ACCOUNT_ID.value(),
            "Lena", "Mustermann", DATE_OF_BIRTH
        );
        when(accountRepository.findById(any(AccountId.class)))
            .thenReturn(Optional.of(IamTestFixtures.account()));
        when(dependentRepository.save(any(Dependent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final DependentRepresentation result = accountService.addDependent(command);

        assertThat(result.firstName()).isEqualTo("Lena");
        assertThat(result.lastName()).isEqualTo("Mustermann");
        assertThat(result.guardianAccountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID.value());
        verify(dependentRepository).save(any(Dependent.class));
        verify(eventPublisher).publishEvent(any(DependentAddedToTenant.class));
    }

    @Test
    void rejectsAddDependentWithUnknownGuardian() {
        final AddDependentCommand command = new AddDependentCommand(
            IamTestFixtures.TENANT_ID.value(), UUID.randomUUID(),
            "Lena", "Mustermann", DATE_OF_BIRTH
        );
        when(accountRepository.findById(any(AccountId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.addDependent(command))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Account not found");
    }

    @Test
    void deleteDependentPublishesEvent() {
        final Dependent dependent = IamTestFixtures.dependent();
        when(dependentRepository.findById(any(DependentId.class)))
            .thenReturn(Optional.of(dependent));
        when(tripParticipationRepository.existsByParticipantId(dependent.dependentId().value())).thenReturn(false);

        accountService.deleteDependent(dependent.dependentId().value());

        verify(eventPublisher).publishEvent(any(DependentRemovedFromTenant.class));
        verify(dependentRepository).deleteById(dependent.dependentId());
    }

    @Test
    void deleteMemberPublishesEvent() {
        final Account account = IamTestFixtures.account();
        when(accountRepository.countByTenantId(IamTestFixtures.TENANT_ID)).thenReturn(2L);
        when(accountRepository.findById(IamTestFixtures.ACCOUNT_ID))
            .thenReturn(Optional.of(account));
        when(tripParticipationRepository.existsByParticipantId(IamTestFixtures.ACCOUNT_ID.value())).thenReturn(false);

        accountService.deleteMember(IamTestFixtures.ACCOUNT_ID, IamTestFixtures.TENANT_ID);

        verify(eventPublisher).publishEvent(any(MemberRemovedFromTenant.class));
        verify(accountRepository).deleteById(IamTestFixtures.ACCOUNT_ID);
    }

    @Test
    void deleteMemberRejectsLastMember() {
        when(accountRepository.countByTenantId(IamTestFixtures.TENANT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> accountService.deleteMember(IamTestFixtures.ACCOUNT_ID, IamTestFixtures.TENANT_ID))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("lastMember");
    }

    @Test
    void deleteMemberRejectsTripParticipants() {
        when(accountRepository.countByTenantId(IamTestFixtures.TENANT_ID)).thenReturn(2L);
        when(tripParticipationRepository.existsByParticipantId(IamTestFixtures.ACCOUNT_ID.value())).thenReturn(true);

        assertThatThrownBy(() -> accountService.deleteMember(IamTestFixtures.ACCOUNT_ID, IamTestFixtures.TENANT_ID))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("tripParticipation");
    }

    @Test
    void deleteDependentRejectsTripParticipants() {
        final UUID dependentId = UUID.randomUUID();
        when(tripParticipationRepository.existsByParticipantId(dependentId)).thenReturn(true);

        assertThatThrownBy(() -> accountService.deleteDependent(dependentId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("tripParticipation");
    }

    @Test
    void inviteMemberCreatesKeycloakUserAndAccount() {
        final InviteMemberCommand command = new InviteMemberCommand(
            IamTestFixtures.TENANT_ID.value(), "invited@example.com",
            "Anna", "Schmidt", DATE_OF_BIRTH
        );
        final KeycloakUserId kcId = new KeycloakUserId("kc-invited-123");
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(false);
        when(identityProviderService.createInvitedUser(any(Email.class), any(FullName.class))).thenReturn(kcId);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationService.generateToken(any(AccountId.class)))
            .thenReturn(InvitationToken.generate(IamTestFixtures.ACCOUNT_ID));

        final InviteMemberResult result = accountService.inviteMember(command);

        assertThat(result.account().email()).isEqualTo("invited@example.com");
        assertThat(result.account().firstName()).isEqualTo("Anna");
        assertThat(result.account().lastName()).isEqualTo("Schmidt");
        assertThat(result.tokenValue()).isNotBlank();
        verify(identityProviderService).createInvitedUser(any(Email.class), any(FullName.class));
        verify(identityProviderService).assignRole(kcId, "organizer");
        verify(registrationService).generateToken(any(AccountId.class));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void inviteMemberRejectsDuplicateEmail() {
        final InviteMemberCommand command = new InviteMemberCommand(
            IamTestFixtures.TENANT_ID.value(), "existing@example.com",
            "Existing", "User", DATE_OF_BIRTH
        );
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(true);

        assertThatThrownBy(() -> accountService.inviteMember(command))
            .isInstanceOf(DuplicateEntityException.class)
            .hasMessageContaining("alreadyExists");
    }

    @Test
    void inviteMemberRollsBackOnKeycloakUserOnFailure() {
        final InviteMemberCommand command = new InviteMemberCommand(
            IamTestFixtures.TENANT_ID.value(), "fail@example.com",
            "Fail", "User", DATE_OF_BIRTH
        );
        final KeycloakUserId kcId = new KeycloakUserId("kc-fail-123");
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(false);
        when(identityProviderService.createInvitedUser(any(Email.class), any(FullName.class))).thenReturn(kcId);
        when(accountRepository.save(any(Account.class))).thenThrow(new RuntimeException("DB error"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accountService.inviteMember(command))
            .isInstanceOf(RuntimeException.class);

        verify(identityProviderService).deleteUser(kcId);
    }

    @Test
    void inviteMemberPublishesAccountRegisteredEvent() {
        final InviteMemberCommand command = new InviteMemberCommand(
            IamTestFixtures.TENANT_ID.value(), "event@example.com",
            "Event", "Test", DATE_OF_BIRTH
        );
        final KeycloakUserId kcId = new KeycloakUserId("kc-event-123");
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(false);
        when(identityProviderService.createInvitedUser(any(Email.class), any(FullName.class))).thenReturn(kcId);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationService.generateToken(any(AccountId.class)))
            .thenReturn(InvitationToken.generate(IamTestFixtures.ACCOUNT_ID));

        accountService.inviteMember(command);

        verify(eventPublisher).publishEvent(any(AccountRegistered.class));
    }

    @Test
    void findsAccountById() {
        final Account account = IamTestFixtures.account();
        when(accountRepository.findById(IamTestFixtures.ACCOUNT_ID)).thenReturn(Optional.of(account));

        final AccountRepresentation result = accountService.findById(IamTestFixtures.ACCOUNT_ID);

        assertThat(result.accountId()).isEqualTo(IamTestFixtures.ACCOUNT_ID.value());
        assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    void findsAllAccountsByTenantId() {
        when(accountRepository.findAllByTenantId(IamTestFixtures.TENANT_ID))
            .thenReturn(List.of(IamTestFixtures.account()));

        final List<AccountRepresentation> result = accountService.findAllByTenantId(IamTestFixtures.TENANT_ID);

        assertThat(result).hasSize(1);
    }
}
