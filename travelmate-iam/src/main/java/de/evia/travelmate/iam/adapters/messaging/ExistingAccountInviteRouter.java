package de.evia.travelmate.iam.adapters.messaging;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.iam.adapters.mail.ReLoginNoticeEmailService;
import de.evia.travelmate.iam.adapters.mail.RegistrationEmailService;
import de.evia.travelmate.iam.adapters.web.RegistrationLinkFactory;
import de.evia.travelmate.iam.application.SignUpService;
import de.evia.travelmate.iam.application.command.RegisterExternalUserCommand;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.IdentityProviderService;
import de.evia.travelmate.iam.domain.account.Username;

@Component
@Profile("!test")
public class ExistingAccountInviteRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ExistingAccountInviteRouter.class);

    private final SignUpService signUpService;
    private final AccountRepository accountRepository;
    private final IdentityProviderService identityProviderService;
    private final RegistrationEmailService registrationEmailService;
    private final ReLoginNoticeEmailService reLoginNoticeEmailService;
    private final RegistrationLinkFactory registrationLinkFactory;
    private final ExternalInviteFollowupTracker followupTracker;
    private final String publicBaseUrl;
    private final String keycloakClientId;

    public ExistingAccountInviteRouter(final SignUpService signUpService,
                                       final AccountRepository accountRepository,
                                       final IdentityProviderService identityProviderService,
                                       final RegistrationEmailService registrationEmailService,
                                       final ReLoginNoticeEmailService reLoginNoticeEmailService,
                                       final RegistrationLinkFactory registrationLinkFactory,
                                       final ExternalInviteFollowupTracker followupTracker,
                                       @Value("${travelmate.public-url:http://localhost:8080}") final String publicBaseUrl,
                                       @Value("${travelmate.keycloak.gateway-client-id:travelmate-gateway}") final String keycloakClientId) {
        this.signUpService = signUpService;
        this.accountRepository = accountRepository;
        this.identityProviderService = identityProviderService;
        this.registrationEmailService = registrationEmailService;
        this.reLoginNoticeEmailService = reLoginNoticeEmailService;
        this.registrationLinkFactory = registrationLinkFactory;
        this.followupTracker = followupTracker;
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
        this.keycloakClientId = keycloakClientId;
    }

    public void route(final String email,
                      final String firstName,
                      final String lastName,
                      final LocalDate dateOfBirth,
                      final String tripName,
                      final UUID tripId,
                      final UUID invitationId,
                      final String inviterFirstName,
                      final String inviterLastName) {
        final Optional<Account> existing = accountRepository.findByUsernameAcrossTenants(new Username(email));

        if (existing.isEmpty()) {
            registerNewAccountAndSendEmail(email, firstName, lastName, dateOfBirth);
            return;
        }

        if (followupTracker.alreadyDispatched(email, tripId)) {
            LOG.info("External invite for {} on trip {} already dispatched, skipping replay", email, tripId);
            return;
        }

        final Account account = existing.get();
        if (identityProviderService.hasPasswordCredential(account.keycloakUserId())) {
            reLoginNoticeEmailService.sendReLoginNotice(
                email, firstName, tripName, inviterFirstName, inviterLastName, loginUrl());
            followupTracker.markDispatched(email, tripId, "RELOGIN_NOTICE");
            LOG.info("Re-login notice sent to {} for trip {}", email, tripName);
            return;
        }

        final URI redirectUri = URI.create(publicBaseUrl + "/trips/invitations/" + invitationId);
        identityProviderService.sendUpdatePasswordEmail(account.keycloakUserId(), redirectUri, keycloakClientId);
        followupTracker.markDispatched(email, tripId, "PASSWORD_SETUP");
        LOG.info("Password setup link sent to {}", email);
    }

    private void registerNewAccountAndSendEmail(final String email,
                                                final String firstName,
                                                final String lastName,
                                                final LocalDate dateOfBirth) {
        final InviteMemberResult result = signUpService.registerExternalUser(
            new RegisterExternalUserCommand(email, firstName, lastName, dateOfBirth));
        final String registrationLink = registrationLinkFactory.registrationLink(result.tokenValue());
        registrationEmailService.sendRegistrationEmail(email, firstName, registrationLink);
        LOG.info("Created new tenant and account for externally invited user {}", email);
    }

    private String loginUrl() {
        return publicBaseUrl + "/oauth2/authorization/keycloak";
    }

    private static String stripTrailingSlash(final String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
