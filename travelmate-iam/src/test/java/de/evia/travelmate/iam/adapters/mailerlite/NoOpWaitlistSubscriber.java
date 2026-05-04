package de.evia.travelmate.iam.adapters.mailerlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.iam.application.marketing.WaitlistSubscriber;

@Component
@Profile("test")
public class NoOpWaitlistSubscriber implements WaitlistSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpWaitlistSubscriber.class);

    @Override
    public void subscribe(final String email) {
        LOG.info("NoOp waitlist subscriber: would subscribe {}", email);
    }
}
