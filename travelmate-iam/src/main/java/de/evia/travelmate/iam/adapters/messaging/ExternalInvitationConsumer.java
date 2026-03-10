package de.evia.travelmate.iam.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Username;

@Component
@Profile("!test")
public class ExternalInvitationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalInvitationConsumer.class);

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    public ExternalInvitationConsumer(final AccountService accountService,
                                     final AccountRepository accountRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_EXTERNAL_USER_INVITED)
    public void onExternalUserInvited(final ExternalUserInvitedToTrip event) {
        final TenantId tenantId = new TenantId(event.tenantId());

        if (accountRepository.existsByUsername(tenantId, new Username(event.email()))) {
            LOG.info("Account already exists for email {} in tenant {}, skipping creation",
                event.email(), event.tenantId());
            return;
        }

        try {
            accountService.inviteMember(new InviteMemberCommand(
                event.tenantId(),
                event.email(),
                event.firstName(),
                event.lastName(),
                event.dateOfBirth()
            ));
            LOG.info("Created account for externally invited user {} in tenant {}",
                event.email(), event.tenantId());
        } catch (final Exception e) {
            LOG.error("Failed to create account for externally invited user {} in tenant {}",
                event.email(), event.tenantId(), e);
            throw e;
        }
    }
}
