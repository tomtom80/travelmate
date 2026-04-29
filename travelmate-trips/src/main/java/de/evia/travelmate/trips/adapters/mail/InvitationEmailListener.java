package de.evia.travelmate.trips.adapters.mail;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.evia.travelmate.common.events.trips.InvitationCreated;

@Component
@Profile("!test")
public class InvitationEmailListener {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationEmailListener.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String baseUrl;
    private final String fromAddress;

    public InvitationEmailListener(final JavaMailSender mailSender,
                                   final TemplateEngine templateEngine,
                                   @Value("${travelmate.base-url}") final String baseUrl,
                                   @Value("${travelmate.mail.from}") final String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = baseUrl;
        this.fromAddress = fromAddress;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationCreated(final InvitationCreated event) {
        final String invitationLink = baseUrl + "/trips/invitations/" + event.invitationId();

        final Context context = new Context();
        context.setVariable("inviteeFirstName", event.inviteeFirstName());
        context.setVariable("tripName", event.tripName());
        context.setVariable("tripStartDate", event.tripStartDate());
        context.setVariable("tripEndDate", event.tripEndDate());
        context.setVariable("inviterFirstName", event.inviterFirstName());
        context.setVariable("inviterLastName", event.inviterLastName());
        context.setVariable("invitationLink", invitationLink);

        final String body = templateEngine.process("email/invitation-member", context);

        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(event.inviteeEmail());
            helper.setSubject("Einladung zur Reise: " + event.tripName());
            helper.setText(body, true);
            mailSender.send(message);
            LOG.info("Invitation email sent to {} for trip {}", event.inviteeEmail(), event.tripName());
        } catch (final MailAuthenticationException e) {
            LOG.error("SMTP authentication failed while sending invitation email to {}. Check SMTP username, password and sender address {}.",
                event.inviteeEmail(), fromAddress, e);
        } catch (final MailSendException e) {
            LOG.error("SMTP transport failed while sending invitation email to {} via configured mail server.",
                event.inviteeEmail(), e);
        } catch (final Exception e) {
            LOG.error("Failed to send invitation email to {} for trip {}",
                event.inviteeEmail(), event.tripName(), e);
        }
    }
}
