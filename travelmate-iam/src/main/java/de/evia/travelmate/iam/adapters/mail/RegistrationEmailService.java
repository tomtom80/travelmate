package de.evia.travelmate.iam.adapters.mail;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@Profile("!test")
public class RegistrationEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationEmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public RegistrationEmailService(final JavaMailSender mailSender,
                                    final TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendRegistrationEmail(final String recipientEmail,
                                      final String firstName,
                                      final String registrationLink) {
        final Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("registrationLink", registrationLink);

        final String body = templateEngine.process("email/member-invitation", context);

        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@travelmate.de");
            helper.setTo(recipientEmail);
            helper.setSubject("Deine Einladung zu Travelmate");
            helper.setText(body, true);
            mailSender.send(message);
            LOG.info("Registration email sent to {}", recipientEmail);
        } catch (final Exception e) {
            LOG.error("Failed to send registration email to {}", recipientEmail, e);
        }
    }
}
