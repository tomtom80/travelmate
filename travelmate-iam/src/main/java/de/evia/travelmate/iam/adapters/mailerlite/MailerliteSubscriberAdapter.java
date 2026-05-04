package de.evia.travelmate.iam.adapters.mailerlite;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import de.evia.travelmate.iam.application.marketing.WaitlistSubscriber;

@Component
@Profile("!test")
public class MailerliteSubscriberAdapter implements WaitlistSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(MailerliteSubscriberAdapter.class);
    private static final String MAILERLITE_BASE_URL = "https://connect.mailerlite.com/api";

    private final RestClient restClient;
    private final String groupId;

    public MailerliteSubscriberAdapter(
            @Value("${mailerlite.api-key:}") final String apiKey,
            @Value("${mailerlite.group-id:}") final String groupId) {
        this.groupId = groupId;
        this.restClient = RestClient.builder()
            .baseUrl(MAILERLITE_BASE_URL)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public void subscribe(final String email) {
        final Map<String, Object> body = buildBody(email);
        try {
            restClient.post()
                .uri("/subscribers")
                .body(body)
                .retrieve()
                .toBodilessEntity();
            LOG.info("Mailerlite: subscribed {}", email);
        } catch (final RestClientResponseException e) {
            final int status = e.getStatusCode().value();
            if (status == 422) {
                // Already subscribed — treat as success (GDPR enumeration defense)
                LOG.info("Mailerlite: {} already subscribed (422), treating as success", email);
                return;
            }
            LOG.error("Mailerlite API error for {}: status={}, body={}", email, status, e.getResponseBodyAsString());
            throw new MailerliteException("Mailerlite API returned " + status + " for " + email, e);
        } catch (final Exception e) {
            LOG.error("Mailerlite request failed for {}: {}", email, e.getMessage());
            throw new MailerliteException("Mailerlite request failed for " + email, e);
        }
    }

    private Map<String, Object> buildBody(final String email) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        if (groupId != null && !groupId.isBlank()) {
            final List<String> groups = new ArrayList<>();
            groups.add(groupId);
            body.put("groups", groups);
        }
        return body;
    }
}
