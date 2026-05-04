package de.evia.travelmate.iam.adapters.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.iam.adapters.mailerlite.MailerliteException;
import de.evia.travelmate.iam.application.marketing.WaitlistSubscriber;

@Controller
public class LandingController {

    private static final Logger LOG = LoggerFactory.getLogger(LandingController.class);
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final WaitlistSubscriber waitlistSubscriber;

    public LandingController(final WaitlistSubscriber waitlistSubscriber) {
        this.waitlistSubscriber = waitlistSubscriber;
    }

    @GetMapping("/landing")
    public String landing(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        model.addAttribute("isAuthenticated", jwt != null);
        return "landing";
    }

    @PostMapping("/landing/waitlist")
    public String submitWaitlist(
            @RequestParam(name = "email", defaultValue = "") final String email,
            @RequestParam(name = "consentGiven", defaultValue = "false") final boolean consentGiven,
            @RequestParam(name = "website", defaultValue = "") final String honeypot) {

        // Honeypot: bots fill this hidden field, humans don't
        if (!honeypot.isBlank()) {
            LOG.info("Waitlist honeypot triggered — silently returning success");
            return "landing/waitlist-success :: fragment";
        }

        if (!consentGiven || email.isBlank() || !email.matches(EMAIL_PATTERN)) {
            return "landing/waitlist-error :: fragment";
        }

        try {
            waitlistSubscriber.subscribe(email);
            return "landing/waitlist-success :: fragment";
        } catch (final MailerliteException e) {
            LOG.error("Waitlist subscription failed for {}", email, e);
            return "landing/waitlist-error :: fragment";
        }
    }
}
