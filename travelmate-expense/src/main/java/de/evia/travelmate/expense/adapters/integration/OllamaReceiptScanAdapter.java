package de.evia.travelmate.expense.adapters.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ReceiptScanPort;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;
import de.evia.travelmate.expense.domain.expense.ScannedLineItem;

/**
 * LLM-based receipt scan adapter using Ollama with Qwen3-VL.
 * Sends the receipt image to Ollama for structured data extraction via vision capabilities.
 * Falls back to returning an empty result if Ollama is unavailable.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "travelmate.llm.enabled", havingValue = "true")
public class OllamaReceiptScanAdapter implements ReceiptScanPort {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaReceiptScanAdapter.class);

    static final String PROMPT = """
        Du bist ein OCR-Assistent fuer deutsche Kassenzettel. Analysiere das Foto \
        und extrahiere diese Informationen als JSON:

        {"totalAmount": 23.45, "date": "YYYY-MM-DD", "storeName": "Name des Geschaefts", \
        "category": "GROCERIES", \
        "lineItems": [{"name": "Artikelname", "price": 1.99}]}

        category muss einer dieser Werte sein: GROCERIES, RESTAURANT, FUEL, HEALTH, \
        ACTIVITY, TRANSPORT, ACCOMMODATION, OTHER

        Antworte NUR mit dem JSON-Objekt. Wenn ein Feld nicht erkennbar ist, setze null.""";

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public OllamaReceiptScanAdapter(
        @Value("${travelmate.llm.base-url:http://localhost:11434}") final String baseUrl,
        @Value("${travelmate.llm.model:qwen3-vl:8b}") final String model,
        @Value("${travelmate.llm.timeout:120}") final int timeoutSeconds
    ) {
        this.ollamaClient = new OllamaClient(baseUrl, model, timeoutSeconds);
        this.objectMapper = new ObjectMapper();
    }

    OllamaReceiptScanAdapter(final OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ReceiptScanResult scan(final byte[] imageData, final String mimeType) {
        if (!ollamaClient.isAvailable()) {
            LOG.info("Ollama is not available for receipt scanning");
            return ReceiptScanResult.failure(
                "KI-Belegerkennung nicht verfuegbar. Bitte Beleg manuell eingeben.");
        }

        final Optional<String> response = ollamaClient.generateWithImage(PROMPT, imageData);

        if (response.isEmpty()) {
            LOG.warn("Ollama returned empty response for receipt scan");
            return ReceiptScanResult.failure(
                "KI-Belegerkennung fehlgeschlagen. Bitte Beleg manuell eingeben.");
        }

        try {
            return parseResponse(response.get());
        } catch (final Exception e) {
            LOG.warn("Failed to parse Ollama receipt response: {}", e.getMessage());
            return ReceiptScanResult.failure(
                "KI-Antwort konnte nicht verarbeitet werden. Bitte Beleg manuell eingeben.");
        }
    }

    ReceiptScanResult parseResponse(final String jsonResponse) {
        try {
            final JsonNode root = objectMapper.readTree(jsonResponse);

            final BigDecimal totalAmount = parsePriceOrNull(root, "totalAmount");
            final LocalDate receiptDate = parseDateOrNull(root, "date");
            final String storeName = textOrNull(root, "storeName");
            final ExpenseCategory category = parseCategoryOrNull(root, "category");
            final List<ScannedLineItem> lineItems = parseLineItems(root);

            return new ReceiptScanResult(
                true,
                totalAmount,
                receiptDate,
                storeName,
                category,
                lineItems,
                jsonResponse,
                null
            );
        } catch (final Exception e) {
            LOG.warn("Failed to parse receipt JSON: {}", e.getMessage());
            return ReceiptScanResult.failure(
                "KI-Antwort konnte nicht verarbeitet werden: " + e.getMessage());
        }
    }

    private String textOrNull(final JsonNode node, final String field) {
        final JsonNode child = node.get(field);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }
        final String text = child.asText().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private LocalDate parseDateOrNull(final JsonNode node, final String field) {
        final String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (final DateTimeParseException e) {
            LOG.debug("Could not parse date '{}' for field '{}'", text, field);
            return null;
        }
    }

    private BigDecimal parsePriceOrNull(final JsonNode node, final String field) {
        final JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isNumber()) {
            return child.decimalValue();
        }
        if (child.isTextual()) {
            try {
                return new BigDecimal(child.asText().trim().replace(",", "."));
            } catch (final NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private ExpenseCategory parseCategoryOrNull(final JsonNode node, final String field) {
        final String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return ExpenseCategory.valueOf(text.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return ExpenseCategory.OTHER;
        }
    }

    private List<ScannedLineItem> parseLineItems(final JsonNode root) {
        final JsonNode itemsNode = root.get("lineItems");
        if (itemsNode == null || !itemsNode.isArray()) {
            return List.of();
        }

        final List<ScannedLineItem> items = new ArrayList<>();
        for (final JsonNode itemNode : itemsNode) {
            final String name = textOrNull(itemNode, "name");
            final BigDecimal price = parsePriceOrNull(itemNode, "price");
            if (name != null && price != null) {
                items.add(new ScannedLineItem(name, price));
            }
        }
        return items;
    }
}
