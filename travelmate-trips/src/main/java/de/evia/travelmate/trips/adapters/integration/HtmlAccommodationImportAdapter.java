package de.evia.travelmate.trips.adapters.integration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.evia.travelmate.trips.domain.accommodation.AccommodationImportPort;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.ImportedRoom;
import de.evia.travelmate.trips.domain.accommodation.RoomType;

@Component
@ConditionalOnProperty(name = "travelmate.llm.enabled", havingValue = "false", matchIfMissing = true)
public class HtmlAccommodationImportAdapter implements AccommodationImportPort {

    private static final Set<String> LODGING_TYPES = Set.of(
        "LodgingBusiness", "Hotel", "VacationRental", "House", "Hostel", "Motel",
        "BedAndBreakfast", "Resort", "Campground"
    );
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d[\\d.,]*)");
    private static final Pattern BEDROOMS_PATTERN = Pattern.compile(
        "(\\d+)\\s*(?:Schlafzimmer|bedrooms?|Zimmer)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEDS_PATTERN = Pattern.compile(
        "(\\d+)\\s*(?:Betten|beds?|Schlafpl[aä]tze)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GUESTS_PATTERN = Pattern.compile(
        "(?:(?:max\\.?\\s*)?(\\d+)\\s*(?:Personen|Gäste|G[aä]ste|guests?|sleeps|accommodates))" +
        "|(?:(?:sleeps|accommodates)\\s*(\\d+))", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_BEDS_PER_ROOM = 2;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int FETCH_TIMEOUT_MS = 5000;
    private static final String USER_AGENT = "Travelmate/0.10.0";

    private final ObjectMapper objectMapper;

    public HtmlAccommodationImportAdapter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<AccommodationImportResult> importFromUrl(final String url) {
        UrlSanitizer.validate(url);
        try {
            final Document document = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(FETCH_TIMEOUT_MS)
                .maxBodySize(5 * 1024 * 1024)
                .followRedirects(true)
                .get();
            return parseHtml(document.html(), url);
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    Optional<AccommodationImportResult> parseHtml(final String html, final String sourceUrl) {
        final Document document = Jsoup.parse(html);

        final Optional<JsonNode> jsonLd = findLodgingJsonLd(document);
        if (jsonLd.isPresent()) {
            return Optional.of(fromJsonLd(jsonLd.get(), sourceUrl, document));
        }

        return fromOpenGraphOrTitle(document, sourceUrl);
    }

    private Optional<JsonNode> findLodgingJsonLd(final Document document) {
        final Elements scripts = document.select("script[type=application/ld+json]");
        for (final Element script : scripts) {
            try {
                final JsonNode root = objectMapper.readTree(script.data());
                final Optional<JsonNode> found = findLodgingNode(root);
                if (found.isPresent()) {
                    return found;
                }
            } catch (final Exception ignored) {
                // Skip malformed JSON-LD
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> findLodgingNode(final JsonNode node) {
        if (node.isArray()) {
            for (final JsonNode element : node) {
                final Optional<JsonNode> found = findLodgingNode(element);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (node.isObject() && node.has("@type")) {
            final String type = node.get("@type").asText();
            if (LODGING_TYPES.contains(type)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private AccommodationImportResult fromJsonLd(final JsonNode jsonLd,
                                                  final String sourceUrl,
                                                  final Document document) {
        final String name = textOrNull(jsonLd, "name");
        final String address = extractAddress(jsonLd);
        final BigDecimal totalPrice = extractPrice(jsonLd);
        final Integer maxGuests = intOrNull(jsonLd, "maximumAttendeeCapacity");
        final List<ImportedRoom> rooms = extractRooms(jsonLd, document);
        final String ogDescription = metaContent(document, "og:description");
        final String notes = truncate(ogDescription, MAX_NOTES_LENGTH);

        return new AccommodationImportResult(
            name != null ? name : metaContent(document, "og:title"),
            address,
            sourceUrl,
            null,
            null,
            totalPrice,
            notes,
            rooms,
            maxGuests
        );
    }

    private Optional<AccommodationImportResult> fromOpenGraphOrTitle(final Document document,
                                                                      final String sourceUrl) {
        final String ogTitle = metaContent(document, "og:title");
        final String ogDescription = metaContent(document, "og:description");
        final String htmlTitle = document.title();

        final String name = ogTitle != null ? ogTitle : htmlTitle;
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        final String notes = truncate(ogDescription, MAX_NOTES_LENGTH);
        final List<ImportedRoom> rooms = extractRoomsFromHtmlBody(document, null);
        final Integer maxGuests = matchGuests(
            document.body() != null ? document.body().text() : "");

        return Optional.of(new AccommodationImportResult(
            name.trim(),
            null,
            sourceUrl,
            null,
            null,
            null,
            notes,
            rooms,
            maxGuests
        ));
    }

    private String extractAddress(final JsonNode jsonLd) {
        final JsonNode addressNode = jsonLd.get("address");
        if (addressNode == null || !addressNode.isObject()) {
            return null;
        }
        final List<String> parts = new ArrayList<>();
        addIfPresent(parts, addressNode, "streetAddress");
        addIfPresent(parts, addressNode, "postalCode");
        addIfPresent(parts, addressNode, "addressLocality");
        addIfPresent(parts, addressNode, "addressRegion");
        addIfPresent(parts, addressNode, "addressCountry");
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private void addIfPresent(final List<String> parts, final JsonNode node, final String field) {
        final String value = textOrNull(node, field);
        if (value != null) {
            parts.add(value);
        }
    }

    private BigDecimal extractPrice(final JsonNode jsonLd) {
        final String priceRange = textOrNull(jsonLd, "priceRange");
        if (priceRange != null) {
            return parseFirstNumber(priceRange);
        }
        final JsonNode offers = jsonLd.get("offers");
        if (offers != null) {
            final String price = textOrNull(offers, "price");
            if (price != null) {
                return parseFirstNumber(price);
            }
        }
        return null;
    }

    private BigDecimal parseFirstNumber(final String text) {
        final Matcher matcher = PRICE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                final String numberStr = matcher.group(1)
                    .replace(".", "")
                    .replace(",", ".");
                return new BigDecimal(numberStr);
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<ImportedRoom> extractRooms(final JsonNode jsonLd, final Document document) {
        // Step 1: Try containsPlace JSON-LD (most detailed)
        final List<ImportedRoom> containsPlaceRooms = extractRoomsFromContainsPlace(jsonLd);
        if (!containsPlaceRooms.isEmpty()) {
            return containsPlaceRooms;
        }

        // Step 2: Try numberOfRooms/numberOfBedrooms from JSON-LD
        final Integer maxGuests = intOrNull(jsonLd, "maximumAttendeeCapacity");
        final List<ImportedRoom> jsonLdRooms = generateRoomsFromJsonLdProperties(jsonLd, maxGuests);
        if (!jsonLdRooms.isEmpty()) {
            return jsonLdRooms;
        }

        // Step 3: HTML body text fallback
        return extractRoomsFromHtmlBody(document, maxGuests);
    }

    private List<ImportedRoom> extractRoomsFromContainsPlace(final JsonNode jsonLd) {
        final JsonNode containsPlace = jsonLd.get("containsPlace");
        if (containsPlace == null || !containsPlace.isArray()) {
            return List.of();
        }

        final List<ImportedRoom> rooms = new ArrayList<>();
        for (final JsonNode roomNode : containsPlace) {
            final String name = textOrNull(roomNode, "name");
            if (name == null) {
                continue;
            }
            final int bedCount = extractBedCount(roomNode);
            final RoomType roomType = inferRoomType(roomNode, bedCount);
            rooms.add(new ImportedRoom(name, roomType, bedCount, null));
        }
        return rooms;
    }

    private List<ImportedRoom> generateRoomsFromJsonLdProperties(final JsonNode jsonLd,
                                                                   final Integer maxGuests) {
        final Integer numberOfRooms = intOrNull(jsonLd, "numberOfRooms");
        final Integer numberOfBedrooms = intOrNull(jsonLd, "numberOfBedrooms");
        final int roomCount = numberOfBedrooms != null ? numberOfBedrooms
            : numberOfRooms != null ? numberOfRooms : 0;

        if (roomCount <= 0) {
            return List.of();
        }

        return generateGenericRooms(roomCount, maxGuests);
    }

    List<ImportedRoom> extractRoomsFromHtmlBody(final Document document, final Integer jsonLdMaxGuests) {
        final String bodyText = document.body() != null ? document.body().text() : "";

        final Integer bedrooms = matchFirstInt(BEDROOMS_PATTERN, bodyText);
        final Integer beds = matchFirstInt(BEDS_PATTERN, bodyText);
        final Integer guests = jsonLdMaxGuests != null ? jsonLdMaxGuests : matchGuests(bodyText);

        if (bedrooms != null && bedrooms > 0) {
            return generateGenericRooms(bedrooms, guests != null ? guests : beds);
        }

        return List.of();
    }

    private List<ImportedRoom> generateGenericRooms(final int roomCount, final Integer totalCapacity) {
        final List<ImportedRoom> rooms = new ArrayList<>();
        for (int i = 1; i <= roomCount; i++) {
            final int bedsPerRoom = estimateBedsPerRoom(roomCount, totalCapacity, i);
            final RoomType roomType = inferRoomTypeFromBedCount(bedsPerRoom);
            rooms.add(new ImportedRoom("Schlafzimmer " + i, roomType, bedsPerRoom, null));
        }
        return rooms;
    }

    private int estimateBedsPerRoom(final int roomCount, final Integer totalCapacity, final int roomIndex) {
        if (totalCapacity == null || totalCapacity <= 0) {
            return DEFAULT_BEDS_PER_ROOM;
        }
        final int baseBedsPerRoom = totalCapacity / roomCount;
        final int remainder = totalCapacity % roomCount;
        // Distribute remainder to the first rooms
        return roomIndex <= remainder ? baseBedsPerRoom + 1 : baseBedsPerRoom;
    }

    private int extractBedCount(final JsonNode roomNode) {
        final JsonNode bed = roomNode.get("bed");
        if (bed != null && bed.has("numberOfBeds")) {
            final int count = bed.get("numberOfBeds").asInt(0);
            if (count > 0) {
                return count;
            }
        }
        return 2;
    }

    private RoomType inferRoomType(final JsonNode roomNode, final int bedCount) {
        final JsonNode bed = roomNode.get("bed");
        if (bed != null && bedCount <= 2) {
            final String typeOfBed = textOrNull(bed, "typeOfBed");
            if (typeOfBed != null) {
                final String lower = typeOfBed.toLowerCase();
                if ((lower.contains("single") || lower.contains("einzel")) && bedCount == 1) {
                    return RoomType.SINGLE;
                }
                if (lower.contains("double") || lower.contains("doppel") || lower.contains("queen") || lower.contains("king")) {
                    return RoomType.DOUBLE;
                }
            }
        }

        if (bedCount == 1) {
            return RoomType.SINGLE;
        }
        if (bedCount == 2) {
            return RoomType.DOUBLE;
        }
        if (bedCount <= 4) {
            return RoomType.QUAD;
        }
        return RoomType.DORMITORY;
    }

    private RoomType inferRoomTypeFromBedCount(final int bedCount) {
        if (bedCount == 1) {
            return RoomType.SINGLE;
        }
        if (bedCount == 2) {
            return RoomType.DOUBLE;
        }
        if (bedCount <= 4) {
            return RoomType.QUAD;
        }
        return RoomType.DORMITORY;
    }

    private Integer intOrNull(final JsonNode node, final String field) {
        final JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        final int value = child.asInt(0);
        return value > 0 ? value : null;
    }

    private Integer matchFirstInt(final Pattern pattern, final String text) {
        final Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer matchGuests(final String text) {
        final Matcher matcher = GUESTS_PATTERN.matcher(text);
        if (matcher.find()) {
            // Group 1 is for "N Personen" pattern, group 2 is for "sleeps N" pattern
            final String group1 = matcher.group(1);
            final String group2 = matcher.group(2);
            try {
                if (group1 != null) {
                    return Integer.parseInt(group1);
                }
                if (group2 != null) {
                    return Integer.parseInt(group2);
                }
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String textOrNull(final JsonNode node, final String field) {
        final JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        final String text = child.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private String metaContent(final Document document, final String property) {
        final Element meta = document.selectFirst("meta[property=" + property + "]");
        if (meta != null) {
            final String content = meta.attr("content");
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return null;
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
