package de.evia.travelmate.iam.adapters.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.adapters.mail.ReLoginNoticeEmailService;
import de.evia.travelmate.iam.adapters.mail.RegistrationEmailService;
import de.evia.travelmate.iam.adapters.web.RegistrationLinkFactory;
import de.evia.travelmate.iam.application.SignUpService;
import de.evia.travelmate.iam.application.command.RegisterExternalUserCommand;
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

@ExtendWith(MockitoExtension.class)
class ExistingAccountInviteRouterTest {

    private static final String EMAIL = "invitee@example.com";
    private static final String FIRST_NAME = "Anna";
    private static final String LAST_NAME = "Beispiel";
    private static final LocalDate DOB = LocalDate.of(1990, 5, 1);
    private static final String TRIP_NAME = "Sommerurlaub 2026";
    private static final UUID TRIP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVITATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String INVITER_FIRST = "Tom";
    private static final String INVITER_LAST = "Klingler";
    private static final String PUBLIC_URL = "http://localhost:8080";
    private static final String CLIENT_ID = "travelmate-gateway";
    private static final KeycloakUserId KC_USER_ID = new KeycloakUserId("kc-1");

    @Mock private SignUpService signUpService;
    @Mock private AccountRepository accountRepository;
    @Mock private IdentityProviderService identityProviderService;
    @Mock private RegistrationEmailService registrationEmailService;
    @Mock private ReLoginNoticeEmailService reLoginNoticeEmailService;
    @Mock private RegistrationLinkFactory registrationLinkFactory;
    @Mock private ExternalInviteFollowupTracker followupTracker;

    private ExistingAccountInviteRouter router;

    @BeforeEach
    void setUp() {
        router = new ExistingAccountInviteRouter(
            signUpService,
            accountRepository,
            identityProviderService,
            registrationEmailService,
            reLoginNoticeEmailService,
            registrationLinkFactory,
            followupTracker,
            PUBLIC_URL,
            CLIENT_ID
        );
    }

    @Test
    void newEmailRegistersAccountAndSendsRegistrationEmail() {
        when(accountRepository.findByUsernameAcrossTenants(new Username(EMAIL)))
            .thenReturn(Optional.empty());
        when(signUpService.registerExternalUser(any(RegisterExternalUserCommand.class)))
            .thenReturn(new InviteMemberResult(null, "token-xyz"));
        when(registrationLinkFactory.registrationLink("token-xyz"))
            .thenReturn(PUBLIC_URL + "/iam/register?token=token-xyz");

        route();

        verify(signUpService, times(1)).registerExternalUser(any());
        verify(registrationEmailService).sendRegistrationEmail(
            eq(EMAIL), eq(FIRST_NAME), eq(PUBLIC_URL + "/iam/register?token=token-xyz"));
        verifyNoInteractions(identityProviderService, reLoginNoticeEmailService);
        verify(followupTracker, never()).markDispatched(anyString(), any(), anyString());
    }

    @Test
    void existingAccountWithoutPasswordSendsUpdatePasswordEmail() {
        givenExistingAccount();
        when(identityProviderService.hasPasswordCredential(KC_USER_ID)).thenReturn(false);
        when(followupTracker.alreadyDispatched(EMAIL, TRIP_ID)).thenReturn(false);

        route();

        final ArgumentCaptor<URI> redirectCaptor = ArgumentCaptor.forClass(URI.class);
        verify(identityProviderService).sendUpdatePasswordEmail(
            eq(KC_USER_ID), redirectCaptor.capture(), eq(CLIENT_ID));
        assertThat(redirectCaptor.getValue().toString())
            .isEqualTo(PUBLIC_URL + "/trips/invitations/" + INVITATION_ID);
        verify(followupTracker).markDispatched(EMAIL, TRIP_ID, "PASSWORD_SETUP");
        verifyNoInteractions(signUpService, registrationEmailService, reLoginNoticeEmailService);
    }

    @Test
    void existingAccountWithPasswordSendsReLoginNotice() {
        givenExistingAccount();
        when(identityProviderService.hasPasswordCredential(KC_USER_ID)).thenReturn(true);
        when(followupTracker.alreadyDispatched(EMAIL, TRIP_ID)).thenReturn(false);

        route();

        verify(reLoginNoticeEmailService).sendReLoginNotice(
            eq(EMAIL),
            eq(FIRST_NAME),
            eq(TRIP_NAME),
            eq(INVITER_FIRST),
            eq(INVITER_LAST),
            eq(PUBLIC_URL + "/oauth2/authorization/keycloak"));
        verify(followupTracker).markDispatched(EMAIL, TRIP_ID, "RELOGIN_NOTICE");
        verify(identityProviderService, never()).sendUpdatePasswordEmail(any(), any(), anyString());
        verifyNoInteractions(signUpService, registrationEmailService);
    }

    @Test
    void replayOnExistingAccountIsIdempotent() {
        givenExistingAccount();
        when(followupTracker.alreadyDispatched(EMAIL, TRIP_ID)).thenReturn(true);

        route();

        verify(identityProviderService, never()).sendUpdatePasswordEmail(any(), any(), anyString());
        verify(reLoginNoticeEmailService, never())
            .sendReLoginNotice(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(followupTracker, never()).markDispatched(anyString(), any(), anyString());
    }

    private void givenExistingAccount() {
        final Account existing = new Account(
            new AccountId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            KC_USER_ID,
            new Username(EMAIL),
            new Email(EMAIL),
            new FullName(FIRST_NAME, LAST_NAME),
            new DateOfBirth(DOB)
        );
        when(accountRepository.findByUsernameAcrossTenants(new Username(EMAIL)))
            .thenReturn(Optional.of(existing));
    }

    private void route() {
        router.route(EMAIL, FIRST_NAME, LAST_NAME, DOB,
            TRIP_NAME, TRIP_ID, INVITATION_ID, INVITER_FIRST, INVITER_LAST);
    }
}
