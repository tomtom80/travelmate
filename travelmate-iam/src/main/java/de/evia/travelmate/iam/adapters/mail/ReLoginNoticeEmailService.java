package de.evia.travelmate.iam.adapters.mail;

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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@Profile("!test")
public class ReLoginNoticeEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(ReLoginNoticeEmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public ReLoginNoticeEmailService(final JavaMailSender mailSender,
                                     final TemplateEngine templateEngine,
                                     @Value("${travelmate.mail.from}") final String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    public void sendReLoginNotice(final String recipientEmail,
                                  final String firstName,
                                  final String tripName,
                                  final String inviterFirstName,
                                  final String inviterLastName,
                                  final String loginUrl) {
        final Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("tripName", tripName);
        context.setVariable("inviterFirstName", inviterFirstName);
        context.setVariable("inviterLastName", inviterLastName);
        context.setVariable("loginUrl", loginUrl);

        final String body = templateEngine.process("email/re-login-notice", context);

        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Du wurdest zu \"" + tripName + "\" eingeladen – jetzt anmelden");
            helper.setText(body, true);
            mailSender.send(message);
            LOG.info("Re-login notice sent to {} for trip {}", recipientEmail, tripName);
        } catch (final MailAuthenticationException e) {
            LOG.error("SMTP authentication failed while sending re-login notice to {}. Check SMTP username, password and sender address {}.",
                recipientEmail, fromAddress, e);
        } catch (final MailSendException e) {
            LOG.error("SMTP transport failed while sending re-login notice to {} via configured mail server.",
                recipientEmail, e);
        } catch (final Exception e) {
            LOG.error("Failed to send re-login notice to {}", recipientEmail, e);
        }
    }
}
