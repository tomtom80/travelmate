package de.evia.travelmate.iam.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.iam.application.command.CompleteRegistrationCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.Password;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
import de.evia.travelmate.iam.domain.registration.InvitationTokenRepository;

@Service
@Transactional
public class RegistrationService {

    private final InvitationTokenRepository tokenRepository;
    private final AccountRepository accountRepository;
    private final IdentityProviderService identityProviderService;

    public RegistrationService(final InvitationTokenRepository tokenRepository,
                               final AccountRepository accountRepository,
                               final IdentityProviderService identityProviderService) {
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
        this.identityProviderService = identityProviderService;
    }

    public InvitationToken generateToken(final AccountId accountId) {
        final InvitationToken token = InvitationToken.generate(accountId);
        return tokenRepository.save(token);
    }

    public void completeRegistration(final CompleteRegistrationCommand command) {
        final InvitationToken token = tokenRepository.findByTokenValue(command.tokenValue())
            .orElseThrow(() -> new EntityNotFoundException("InvitationToken", command.tokenValue()));

        token.use();

        final Account account = accountRepository.findById(token.accountId())
            .orElseThrow(() -> new EntityNotFoundException("Account", token.accountId().value().toString()));

        final Password password = new Password(command.password());
        identityProviderService.setPassword(account.keycloakUserId(), password);
        identityProviderService.setEmailVerified(account.keycloakUserId(), true);
        tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public InvitationToken findByTokenValue(final String tokenValue) {
        return tokenRepository.findByTokenValue(tokenValue)
            .orElseThrow(() -> new EntityNotFoundException("InvitationToken", tokenValue));
    }
}
