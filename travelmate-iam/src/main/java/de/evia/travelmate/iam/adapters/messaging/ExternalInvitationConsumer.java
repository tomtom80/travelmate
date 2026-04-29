package de.evia.travelmate.iam.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;
import de.evia.travelmate.iam.adapters.mail.RegistrationEmailService;
import de.evia.travelmate.iam.adapters.web.RegistrationLinkFactory;
import de.evia.travelmate.iam.application.SignUpService;
import de.evia.travelmate.iam.application.command.RegisterExternalUserCommand;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Username;

@Component
@Profile("!test")
public class ExternalInvitationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalInvitationConsumer.class);

    private final SignUpService signUpService;
    private final AccountRepository accountRepository;
    private final RegistrationEmailService registrationEmailService;
    private final RegistrationLinkFactory registrationLinkFactory;
    private final Timer externalUserInvitedTimer;

    public ExternalInvitationConsumer(final SignUpService signUpService,
                                     final AccountRepository accountRepository,
                                     final RegistrationEmailService registrationEmailService,
                                     final RegistrationLinkFactory registrationLinkFactory,
                                     final MeterRegistry meterRegistry) {
        this.signUpService = signUpService;
        this.accountRepository = accountRepository;
        this.registrationEmailService = registrationEmailService;
        this.registrationLinkFactory = registrationLinkFactory;
        this.externalUserInvitedTimer = Timer.builder("travelmate.event.processing")
            .tag("scs", "iam")
            .tag("event", "ExternalUserInvitedToTrip")
            .description("Time spent processing domain events")
            .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_EXTERNAL_USER_INVITED)
    public void onExternalUserInvited(final ExternalUserInvitedToTrip event) {
        externalUserInvitedTimer.record(() -> processExternalUserInvited(event));
    }

    private void processExternalUserInvited(final ExternalUserInvitedToTrip event) {
        if (accountRepository.existsByUsernameAcrossTenants(new Username(event.email()))) {
            LOG.info("Account already exists for email {}, skipping creation", event.email());
            return;
        }

        try {
            final InviteMemberResult result = signUpService.registerExternalUser(
                new RegisterExternalUserCommand(
                    event.email(),
                    event.firstName(),
                    event.lastName(),
                    event.dateOfBirth()
                )
            );
            final String registrationLink = registrationLinkFactory.registrationLink(result.tokenValue());
            registrationEmailService.sendRegistrationEmail(event.email(), event.firstName(), registrationLink);
            LOG.info("Created new tenant and account for externally invited user {}", event.email());
        } catch (final Exception e) {
            LOG.error("Failed to create tenant/account for externally invited user {}",
                event.email(), e);
            throw e;
        }
    }
}
