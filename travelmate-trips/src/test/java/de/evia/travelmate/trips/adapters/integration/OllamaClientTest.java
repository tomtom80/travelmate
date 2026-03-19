package de.evia.travelmate.trips.adapters.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OllamaClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildRequestBodyContainsModelAndPrompt() throws Exception {
        final OllamaClient client = new OllamaClient("http://localhost:11434", "qwen3-vl:8b", 120);

        final String body = client.buildRequestBody("Extract data", null);

        final JsonNode root = objectMapper.readTree(body);
        assertThat(root.get("model").asText()).isEqualTo("qwen3-vl:8b");
        assertThat(root.get("prompt").asText()).isEqualTo("Extract data");
        assertThat(root.get("format").asText()).isEqualTo("json");
        assertThat(root.get("stream").asBoolean()).isFalse();
        assertThat(root.get("options").get("temperature").asInt()).isZero();
    }

    @Test
    void buildRequestBodyWithoutImageOmitsImagesField() throws Exception {
        final OllamaClient client = new OllamaClient("http://localhost:11434", "qwen3-vl:8b", 120);

        final String body = client.buildRequestBody("test prompt", null);

        final JsonNode root = objectMapper.readTree(body);
        assertThat(root.has("images")).isFalse();
    }

    @Test
    void buildRequestBodyWithImageIncludesBase64Images() throws Exception {
        final OllamaClient client = new OllamaClient("http://localhost:11434", "qwen3-vl:8b", 120);

        final String body = client.buildRequestBody("analyze image", "aW1hZ2VkYXRh");

        final JsonNode root = objectMapper.readTree(body);
        assertThat(root.has("images")).isTrue();
        assertThat(root.get("images").isArray()).isTrue();
        assertThat(root.get("images")).hasSize(1);
        assertThat(root.get("images").get(0).asText()).isEqualTo("aW1hZ2VkYXRh");
    }

    @Test
    void isAvailableReturnsFalseWhenOllamaNotRunning() {
        final OllamaClient client = new OllamaClient("http://localhost:19999", "qwen3-vl:8b", 5);

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void generateReturnsEmptyWhenOllamaNotRunning() {
        final OllamaClient client = new OllamaClient("http://localhost:19999", "qwen3-vl:8b", 5);

        assertThat(client.generate("test")).isEmpty();
    }

    @Test
    void generateWithImageReturnsEmptyWhenOllamaNotRunning() {
        final OllamaClient client = new OllamaClient("http://localhost:19999", "qwen3-vl:8b", 5);

        assertThat(client.generateWithImage("test", new byte[]{1, 2, 3})).isEmpty();
    }
}
