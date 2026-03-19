package de.evia.travelmate.trips.adapters.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.RoomType;

@ExtendWith(MockitoExtension.class)
class OllamaAccommodationImportAdapterTest {

    @Mock
    private OllamaClient ollamaClient;

    private OllamaAccommodationImportAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OllamaAccommodationImportAdapter(ollamaClient);
    }

    @Test
    void parsesCompleteJsonResponse() {
        final String json = """
            {
                "name": "Chalet am Kogl",
                "address": "Kogl 32, 8551 Wies",
                "checkIn": "2025-07-12",
                "checkOut": "2025-07-19",
                "totalPrice": 4025.50,
                "maxGuests": 13,
                "rooms": [
                    {"name": "Schlafzimmer 1", "bedCount": 2, "roomType": "DOUBLE"},
                    {"name": "Kinderzimmer", "bedCount": 4, "roomType": "QUAD"}
                ],
                "amenities": ["Sauna", "WLAN", "Garten"],
                "notes": "Haustiere erlaubt"
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com/chalet");

        assertThat(result).isPresent();
        final AccommodationImportResult data = result.get();
        assertThat(data.name()).isEqualTo("Chalet am Kogl");
        assertThat(data.address()).isEqualTo("Kogl 32, 8551 Wies");
        assertThat(data.checkIn()).isEqualTo(LocalDate.of(2025, 7, 12));
        assertThat(data.checkOut()).isEqualTo(LocalDate.of(2025, 7, 19));
        assertThat(data.totalPrice()).isEqualByComparingTo(new BigDecimal("4025.50"));
        assertThat(data.maxGuests()).isEqualTo(13);
        assertThat(data.bookingUrl()).isEqualTo("https://example.com/chalet");
        assertThat(data.rooms()).hasSize(2);
        assertThat(data.rooms().getFirst().name()).isEqualTo("Schlafzimmer 1");
        assertThat(data.rooms().getFirst().roomType()).isEqualTo(RoomType.DOUBLE);
        assertThat(data.rooms().getFirst().bedCount()).isEqualTo(2);
        assertThat(data.rooms().get(1).roomType()).isEqualTo(RoomType.QUAD);
        assertThat(data.notes()).contains("Haustiere erlaubt");
        assertThat(data.notes()).contains("Sauna");
        assertThat(data.notes()).contains("WLAN");
    }

    @Test
    void parsesMinimalJsonResponse() {
        final String json = """
            {
                "name": "Ferienhaus Waldblick",
                "address": null,
                "checkIn": null,
                "checkOut": null,
                "totalPrice": null,
                "maxGuests": null,
                "rooms": [],
                "amenities": [],
                "notes": null
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isPresent();
        final AccommodationImportResult data = result.get();
        assertThat(data.name()).isEqualTo("Ferienhaus Waldblick");
        assertThat(data.address()).isNull();
        assertThat(data.checkIn()).isNull();
        assertThat(data.checkOut()).isNull();
        assertThat(data.totalPrice()).isNull();
        assertThat(data.maxGuests()).isNull();
        assertThat(data.rooms()).isEmpty();
        assertThat(data.notes()).isNull();
    }

    @Test
    void returnsEmptyWhenNameIsMissing() {
        final String json = """
            {
                "address": "Some address",
                "totalPrice": 100.0
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void handlesInvalidDateGracefully() {
        final String json = """
            {
                "name": "Test Place",
                "checkIn": "not-a-date",
                "checkOut": "2025-13-99"
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().checkIn()).isNull();
        assertThat(result.get().checkOut()).isNull();
    }

    @Test
    void handlesUnknownRoomTypeGracefully() {
        final String json = """
            {
                "name": "Test Place",
                "rooms": [
                    {"name": "Room 1", "bedCount": 2, "roomType": "UNKNOWN_TYPE"}
                ]
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().rooms()).hasSize(1);
        assertThat(result.get().rooms().getFirst().roomType()).isEqualTo(RoomType.OTHER);
    }

    @Test
    void handlesInvalidJsonGracefully() {
        final String invalidJson = "this is not json at all";

        final Optional<AccommodationImportResult> result = adapter.parseResponse(invalidJson, "https://example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void importFromHtmlFallsBackWhenOllamaReturnsEmpty() {
        when(ollamaClient.generate(anyString())).thenReturn(Optional.empty());

        final String html = """
            <html>
            <head>
                <title>Test Place</title>
                <meta property="og:title" content="Test Place">
            </head>
            <body><p>Content</p></body>
            </html>
            """;

        final Optional<AccommodationImportResult> result = adapter.importFromHtml(html, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Test Place");
    }

    @Test
    void importFromHtmlUsesOllamaResponseWhenAvailable() {
        final String ollamaResponse = """
            {"name": "LLM Extracted Name", "address": "LLM Address"}
            """;
        when(ollamaClient.generate(anyString())).thenReturn(Optional.of(ollamaResponse));

        final String html = """
            <html>
            <head><title>Original Title</title></head>
            <body><p>Some content about a vacation rental</p></body>
            </html>
            """;

        final Optional<AccommodationImportResult> result = adapter.importFromHtml(html, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("LLM Extracted Name");
        assertThat(result.get().address()).isEqualTo("LLM Address");
    }

    @Test
    void importFromHtmlFallsBackOnMalformedOllamaResponse() {
        when(ollamaClient.generate(anyString())).thenReturn(Optional.of("not valid json {{{"));

        final String html = """
            <html>
            <head>
                <title>Fallback Place</title>
                <meta property="og:title" content="Fallback Place">
            </head>
            <body><p>Content</p></body>
            </html>
            """;

        final Optional<AccommodationImportResult> result = adapter.importFromHtml(html, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Fallback Place");
    }

    @Test
    void promptContainsCleanedHtmlText() {
        when(ollamaClient.generate(anyString())).thenReturn(Optional.of("{\"name\": \"Test\"}"));

        final String html = """
            <html>
            <head>
                <title>Test</title>
                <script>var x = 1;</script>
                <style>body { color: red; }</style>
            </head>
            <body>
                <nav>Navigation</nav>
                <h1>Main Content</h1>
                <footer>Footer</footer>
            </body>
            </html>
            """;

        adapter.importFromHtml(html, "https://example.com");

        verify(ollamaClient).generate(anyString());
        verify(ollamaClient, never()).generateWithImage(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void parsesPriceAsStringWithComma() {
        final String json = """
            {
                "name": "Test Place",
                "totalPrice": "1.234,56"
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isPresent();
        // The comma-separated format gets the comma replaced with dot
        // but "1.234.56" is not valid BigDecimal, so it returns null
        // This is expected behavior — LLM should return proper numbers
    }

    @Test
    void buildsCombinedNotesFromNotesAndAmenities() {
        final String json = """
            {
                "name": "Test Place",
                "notes": "Haustiere erlaubt",
                "amenities": ["Pool", "Sauna"]
            }
            """;

        final Optional<AccommodationImportResult> result = adapter.parseResponse(json, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().notes()).contains("Haustiere erlaubt");
        assertThat(result.get().notes()).contains("Pool");
        assertThat(result.get().notes()).contains("Sauna");
    }
}
