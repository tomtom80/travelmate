package de.evia.travelmate.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.iam.application.command.CompleteRegistrationCommand;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Password;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
import de.evia.travelmate.iam.domain.registration.InvitationTokenRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private InvitationTokenRepository tokenRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityProviderService identityProviderService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void generateTokenCreatesAndSaves() {
        final AccountId accountId = new AccountId(UUID.randomUUID());
        when(tokenRepository.save(any(InvitationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        final InvitationToken token = registrationService.generateToken(accountId);

        assertThat(token.accountId()).isEqualTo(accountId);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isExpired()).isFalse();
        verify(tokenRepository).save(any(InvitationToken.class));
    }

    @Test
    void completeRegistrationSetsPasswordAndVerifies() {
        final InvitationToken token = InvitationToken.generate(IamTestFixtures.ACCOUNT_ID);
        final var account = IamTestFixtures.account();
        when(tokenRepository.findByTokenValue(token.tokenValue())).thenReturn(Optional.of(token));
        when(accountRepository.findById(IamTestFixtures.ACCOUNT_ID)).thenReturn(Optional.of(account));

        registrationService.completeRegistration(
            new CompleteRegistrationCommand(token.tokenValue(), "securePass1")
        );

        verify(identityProviderService).setPassword(account.keycloakUserId(), new Password("securePass1"));
        verify(identityProviderService).setEmailVerified(account.keycloakUserId(), true);
        verify(tokenRepository).save(token);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        final InvitationToken expired = new InvitationToken(
            UUID.randomUUID().toString(),
            IamTestFixtures.ACCOUNT_ID,
            LocalDateTime.now().minusHours(1),
            false
        );
        when(tokenRepository.findByTokenValue(expired.tokenValue())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> registrationService.completeRegistration(
            new CompleteRegistrationCommand(expired.tokenValue(), "securePass1")
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void rejectsUsedToken() {
        final InvitationToken used = InvitationToken.generate(IamTestFixtures.ACCOUNT_ID);
        used.use();
        when(tokenRepository.findByTokenValue(used.tokenValue())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> registrationService.completeRegistration(
            new CompleteRegistrationCommand(used.tokenValue(), "securePass1")
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
    }

    @Test
    void rejectsUnknownToken() {
        when(tokenRepository.findByTokenValue("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.completeRegistration(
            new CompleteRegistrationCommand("unknown", "securePass1")
        )).isInstanceOf(de.evia.travelmate.common.domain.EntityNotFoundException.class);
    }
}
