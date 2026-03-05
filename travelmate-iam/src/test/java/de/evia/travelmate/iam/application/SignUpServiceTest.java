package de.evia.travelmate.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.iam.application.command.SignUpCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

    private static final String TENANT_NAME = "Hüttenurlaub 2026";
    private static final String FIRST_NAME = "Max";
    private static final String LAST_NAME = "Mustermann";
    private static final String EMAIL = "max@example.com";
    private static final String PASSWORD = "secureP4ss!";
    private static final KeycloakUserId KEYCLOAK_USER_ID = new KeycloakUserId("kc-user-123");

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SignUpService signUpService;

    @Test
    void signUpCreatesTenantAndAccount() {
        final SignUpCommand command = new SignUpCommand(
            TENANT_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD
        );
        when(tenantRepository.existsByName(new TenantName(TENANT_NAME))).thenReturn(false);
        when(identityProviderService.createUser(
            eq(new Email(EMAIL)),
            eq(new FullName(FIRST_NAME, LAST_NAME)),
            eq(new Password(PASSWORD))
        )).thenReturn(KEYCLOAK_USER_ID);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        signUpService.signUp(command);

        verify(tenantRepository).save(any(Tenant.class));
        verify(identityProviderService).createUser(any(), any(), any());
        verify(accountRepository).save(any(Account.class));
        verify(identityProviderService).assignRole(eq(KEYCLOAK_USER_ID), eq("organizer"));
    }

    @Test
    void signUpPublishesTenantCreatedAndAccountRegisteredEvents() {
        final SignUpCommand command = new SignUpCommand(
            TENANT_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD
        );
        when(tenantRepository.existsByName(new TenantName(TENANT_NAME))).thenReturn(false);
        when(identityProviderService.createUser(any(), any(), any())).thenReturn(KEYCLOAK_USER_ID);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        signUpService.signUp(command);

        final var eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publishEvent(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events).hasAtLeastOneElementOfType(TenantCreated.class);
        assertThat(events).hasAtLeastOneElementOfType(AccountRegistered.class);
    }

    @Test
    void signUpRejectsDuplicateTenantName() {
        final SignUpCommand command = new SignUpCommand(
            TENANT_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD
        );
        when(tenantRepository.existsByName(new TenantName(TENANT_NAME))).thenReturn(true);

        assertThatThrownBy(() -> signUpService.signUp(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(TENANT_NAME);

        verify(identityProviderService, never()).createUser(any(), any(), any());
    }

    @Test
    void signUpDeletesKeycloakUserOnAccountSaveFailure() {
        final SignUpCommand command = new SignUpCommand(
            TENANT_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD
        );
        when(tenantRepository.existsByName(new TenantName(TENANT_NAME))).thenReturn(false);
        when(identityProviderService.createUser(any(), any(), any())).thenReturn(KEYCLOAK_USER_ID);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> signUpService.signUp(command))
            .isInstanceOf(RuntimeException.class);

        verify(identityProviderService).deleteUser(KEYCLOAK_USER_ID);
    }
}
