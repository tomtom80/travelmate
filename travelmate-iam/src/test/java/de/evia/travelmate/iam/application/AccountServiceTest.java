package de.evia.travelmate.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Username;
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DependentRepository dependentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    @Test
    void registersAccount() {
        final RegisterAccountCommand command = new RegisterAccountCommand(
            IamTestFixtures.TENANT_ID.value(), "kc-123", "testuser",
            "test@example.com", "Max", "Mustermann"
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
            "test@example.com", "Max", "Mustermann"
        );
        when(accountRepository.existsByUsername(any(TenantId.class), any(Username.class))).thenReturn(true);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> accountService.registerAccount(command))
            .withMessageContaining("already taken");
    }

    @Test
    void addsDependentToAccount() {
        final AddDependentCommand command = new AddDependentCommand(
            IamTestFixtures.TENANT_ID.value(), IamTestFixtures.ACCOUNT_ID.value(),
            "Lena", "Mustermann"
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
            "Lena", "Mustermann"
        );
        when(accountRepository.findById(any(AccountId.class))).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
            .isThrownBy(() -> accountService.addDependent(command))
            .withMessageContaining("Guardian account not found");
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
