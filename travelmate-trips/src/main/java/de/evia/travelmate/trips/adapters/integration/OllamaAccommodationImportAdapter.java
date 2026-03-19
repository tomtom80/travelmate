package de.evia.travelmate.trips.adapters.integration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.evia.travelmate.trips.domain.accommodation.AccommodationImportPort;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.ImportedRoom;
import de.evia.travelmate.trips.domain.accommodation.RoomType;

/**
 * LLM-based accommodation import adapter using Ollama with Qwen3-VL.
 * Fetches HTML via Jsoup (with SSRF protection), cleans it, and sends the text
 * to Ollama for structured data extraction.
 * Falls back to {@link HtmlAccommodationImportAdapter} logic if Ollama is unavailable.
 */
@Component
@ConditionalOnProperty(name = "travelmate.llm.enabled", havingValue = "true")
public class OllamaAccommodationImportAdapter implements AccommodationImportPort {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaAccommodationImportAdapter.class);

    private static final int FETCH_TIMEOUT_MS = 5000;
    private static final int MAX_BODY_SIZE = 5 * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 15000;
    private static final String USER_AGENT = "Travelmate/0.10.0";

    static final String PROMPT_TEMPLATE = """
        Du bist ein Datenextraktions-Assistent. Extrahiere aus dem folgenden HTML-Text \
        einer Ferienhausseite diese Informationen als JSON:

        {"name": "Name der Unterkunft", "address": "Vollstaendige Adresse", \
        "checkIn": "YYYY-MM-DD oder null", "checkOut": "YYYY-MM-DD oder null", \
        "totalPrice": 1234.56, "maxGuests": 8, \
        "rooms": [{"name": "Schlafzimmer 1", "bedCount": 2, "roomType": "DOUBLE"}], \
        "amenities": ["Sauna", "WLAN", "Garten"], \
        "notes": "Wichtige Zusatzinfos"}

        roomType muss einer dieser Werte sein: SINGLE, DOUBLE, QUAD, DORMITORY, OTHER

        Antworte NUR mit dem JSON-Objekt, keine Erklaerungen.

        --- HTML START ---
        %s
        --- HTML END ---""";

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final HtmlAccommodationImportAdapter fallbackAdapter;

    public OllamaAccommodationImportAdapter(
        @Value("${travelmate.llm.base-url:http://localhost:11434}") final String baseUrl,
        @Value("${travelmate.llm.model:qwen3-vl:8b}") final String model,
        @Value("${travelmate.llm.timeout:120}") final int timeoutSeconds
    ) {
        this.ollamaClient = new OllamaClient(baseUrl, model, timeoutSeconds);
        this.objectMapper = new ObjectMapper();
        this.fallbackAdapter = new HtmlAccommodationImportAdapter();
    }

    OllamaAccommodationImportAdapter(final OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();
        this.fallbackAdapter = new HtmlAccommodationImportAdapter();
    }

    @Override
    public Optional<AccommodationImportResult> importFromUrl(final String url) {
        UrlSanitizer.validate(url);

        if (!ollamaClient.isAvailable()) {
            LOG.info("Ollama is not available, falling back to rule-based adapter for URL: {}", url);
            return fallbackAdapter.importFromUrl(url);
        }

        try {
            final Document document = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(FETCH_TIMEOUT_MS)
                .maxBodySize(MAX_BODY_SIZE)
                .followRedirects(true)
                .get();

            return importFromHtml(document.html(), url);
        } catch (final IOException e) {
            LOG.warn("Failed to fetch URL {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<AccommodationImportResult> importFromHtml(final String html, final String sourceUrl) {
        final String cleanedText = cleanHtml(html);
        final String truncatedText = cleanedText.length() > MAX_TEXT_LENGTH
            ? cleanedText.substring(0, MAX_TEXT_LENGTH)
            : cleanedText;

        final String prompt = String.format(PROMPT_TEMPLATE, truncatedText);
        final Optional<String> response = ollamaClient.generate(prompt);

        if (response.isEmpty()) {
            LOG.warn("Ollama returned empty response, falling back to rule-based adapter");
            return fallbackAdapter.parseHtml(html, sourceUrl);
        }

        final Optional<AccommodationImportResult> result = parseResponse(response.get(), sourceUrl);
        if (result.isEmpty()) {
            LOG.warn("Failed to parse Ollama response, falling back to rule-based adapter");
            return fallbackAdapter.parseHtml(html, sourceUrl);
        }
        return result;
    }

    Optional<AccommodationImportResult> parseResponse(final String jsonResponse, final String sourceUrl) {
        try {
            final JsonNode root = objectMapper.readTree(jsonResponse);

            final String name = textOrNull(root, "name");
            if (name == null) {
                return Optional.empty();
            }

            final String address = textOrNull(root, "address");
            final LocalDate checkIn = parseDateOrNull(root, "checkIn");
            final LocalDate checkOut = parseDateOrNull(root, "checkOut");
            final BigDecimal totalPrice = parsePriceOrNull(root, "totalPrice");
            final Integer maxGuests = intOrNull(root, "maxGuests");
            final String notes = buildNotes(root);
            final List<ImportedRoom> rooms = parseRooms(root);

            return Optional.of(new AccommodationImportResult(
                name,
                address,
                sourceUrl,
                checkIn,
                checkOut,
                totalPrice,
                notes,
                rooms,
                maxGuests
            ));
        } catch (final Exception e) {
            LOG.warn("Failed to parse JSON response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String cleanHtml(final String html) {
        final Document document = Jsoup.parse(html);
        document.select("script, style, nav, footer, header, iframe, noscript").remove();
        return document.body() != null ? document.body().text() : document.text();
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

    private Integer intOrNull(final JsonNode node, final String field) {
        final JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isNumber()) {
            final int value = child.asInt(0);
            return value > 0 ? value : null;
        }
        return null;
    }

    private String buildNotes(final JsonNode root) {
        final StringBuilder notes = new StringBuilder();
        final String rawNotes = textOrNull(root, "notes");
        if (rawNotes != null) {
            notes.append(rawNotes);
        }
        final JsonNode amenities = root.get("amenities");
        if (amenities != null && amenities.isArray() && !amenities.isEmpty()) {
            if (!notes.isEmpty()) {
                notes.append("\n");
            }
            notes.append("Ausstattung: ");
            final List<String> amenityList = new ArrayList<>();
            for (final JsonNode amenity : amenities) {
                if (amenity.isTextual() && !amenity.asText().isBlank()) {
                    amenityList.add(amenity.asText().trim());
                }
            }
            notes.append(String.join(", ", amenityList));
        }
        return notes.isEmpty() ? null : notes.toString();
    }

    private List<ImportedRoom> parseRooms(final JsonNode root) {
        final JsonNode roomsNode = root.get("rooms");
        if (roomsNode == null || !roomsNode.isArray()) {
            return List.of();
        }

        final List<ImportedRoom> rooms = new ArrayList<>();
        for (final JsonNode roomNode : roomsNode) {
            final String name = textOrNull(roomNode, "name");
            if (name == null) {
                continue;
            }

            final int bedCount = roomNode.has("bedCount") && roomNode.get("bedCount").isNumber()
                ? roomNode.get("bedCount").asInt(2) : 2;

            final RoomType roomType = parseRoomType(roomNode);

            rooms.add(new ImportedRoom(name, roomType, bedCount, null));
        }
        return rooms;
    }

    private RoomType parseRoomType(final JsonNode roomNode) {
        final String typeStr = textOrNull(roomNode, "roomType");
        if (typeStr == null) {
            return RoomType.OTHER;
        }
        try {
            return RoomType.valueOf(typeStr.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return RoomType.OTHER;
        }
    }
}
