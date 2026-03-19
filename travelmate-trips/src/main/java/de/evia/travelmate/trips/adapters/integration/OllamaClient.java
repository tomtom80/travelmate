package de.evia.travelmate.trips.adapters.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Thin REST client for the Ollama /api/generate endpoint.
 * Uses java.net.http.HttpClient (JDK built-in, no extra dependency).
 */
class OllamaClient {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaClient.class);

    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    OllamaClient(final String baseUrl, final String model, final int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Text-only generation (for HTML extraction).
     */
    Optional<String> generate(final String prompt) {
        return doGenerate(prompt, null);
    }

    /**
     * Vision generation (for image analysis).
     */
    Optional<String> generateWithImage(final String prompt, final byte[] imageData) {
        final String base64Image = Base64.getEncoder().encodeToString(imageData);
        return doGenerate(prompt, base64Image);
    }

    /**
     * Health check: returns true if Ollama is reachable.
     */
    boolean isAvailable() {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (final Exception e) {
            LOG.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Optional<String> doGenerate(final String prompt, final String base64Image) {
        try {
            final String requestBody = buildRequestBody(prompt, base64Image);

            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

            LOG.debug("Sending Ollama request to {} with model {}", baseUrl, model);

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.warn("Ollama returned status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            final JsonNode responseNode = objectMapper.readTree(response.body());
            final JsonNode responseField = responseNode.get("response");
            if (responseField == null || responseField.isNull()) {
                LOG.warn("Ollama response has no 'response' field");
                return Optional.empty();
            }

            final String responseText = responseField.asText().trim();
            return responseText.isEmpty() ? Optional.empty() : Optional.of(responseText);
        } catch (final Exception e) {
            LOG.warn("Ollama request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    String buildRequestBody(final String prompt, final String base64Image) {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("prompt", prompt);
        root.put("format", "json");
        root.put("stream", false);

        final ObjectNode options = objectMapper.createObjectNode();
        options.put("temperature", 0);
        root.set("options", options);

        if (base64Image != null) {
            final ArrayNode images = objectMapper.createArrayNode();
            images.add(base64Image);
            root.set("images", images);
        }

        return root.toString();
    }
}
