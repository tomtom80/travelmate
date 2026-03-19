package de.evia.travelmate.trips.adapters.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.RoomType;

class HtmlAccommodationImportAdapterTest {

    private static final String JSON_LD_LODGING_BUSINESS = """
        <html>
        <head>
            <title>Chalet am Kogl - huetten.com</title>
            <meta property="og:title" content="Chalet am Kogl">
            <meta property="og:description" content="Traumhafte Huette in den Bergen mit Sauna und Pool">
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "LodgingBusiness",
                "name": "Chalet am Kogl",
                "address": {
                    "@type": "PostalAddress",
                    "streetAddress": "Kogl 32",
                    "postalCode": "8551",
                    "addressLocality": "Wies"
                },
                "maximumAttendeeCapacity": 13,
                "priceRange": "ab 4025 EUR pro Woche",
                "numberOfRooms": 5,
                "containsPlace": [
                    {
                        "@type": "Room",
                        "name": "Schlafzimmer 1",
                        "description": "Doppelbett",
                        "numberOfBedrooms": 1,
                        "bed": { "@type": "BedDetails", "numberOfBeds": 2, "typeOfBed": "Double" }
                    },
                    {
                        "@type": "Room",
                        "name": "Schlafzimmer 2",
                        "description": "Einzelbett",
                        "numberOfBedrooms": 1,
                        "bed": { "@type": "BedDetails", "numberOfBeds": 1, "typeOfBed": "Single" }
                    },
                    {
                        "@type": "Room",
                        "name": "Kinderzimmer",
                        "description": "4 Betten",
                        "numberOfBedrooms": 1,
                        "bed": { "@type": "BedDetails", "numberOfBeds": 4, "typeOfBed": "Single" }
                    }
                ]
            }
            </script>
        </head>
        <body><h1>Chalet am Kogl</h1></body>
        </html>
        """;

    private static final String JSON_LD_VACATION_RENTAL = """
        <html>
        <head>
            <title>Cozy Apartment - Airbnb</title>
            <meta property="og:title" content="Cozy Mountain Apartment">
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "VacationRental",
                "name": "Cozy Mountain Apartment",
                "address": {
                    "@type": "PostalAddress",
                    "streetAddress": "Bergweg 5",
                    "addressLocality": "Innsbruck"
                }
            }
            </script>
        </head>
        <body><h1>Cozy Mountain Apartment</h1></body>
        </html>
        """;

    private static final String OPEN_GRAPH_ONLY = """
        <html>
        <head>
            <title>Ferienhaus Waldblick</title>
            <meta property="og:title" content="Ferienhaus Waldblick">
            <meta property="og:description" content="Schoenes Ferienhaus am Waldrand mit 4 Schlafzimmern.">
        </head>
        <body><h1>Ferienhaus Waldblick</h1></body>
        </html>
        """;

    private static final String TITLE_ONLY = """
        <html>
        <head>
            <title>Some Vacation Place</title>
        </head>
        <body><p>Content here</p></body>
        </html>
        """;

    private static final String EMPTY_PAGE = """
        <html>
        <head></head>
        <body></body>
        </html>
        """;

    private static final String JSON_LD_WITH_PRICE_NUMBER = """
        <html>
        <head>
            <title>Hotel Test</title>
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "Hotel",
                "name": "Hotel Alpenrose",
                "address": {
                    "@type": "PostalAddress",
                    "streetAddress": "Hauptstr. 1",
                    "addressLocality": "Garmisch"
                },
                "priceRange": "2500"
            }
            </script>
        </head>
        <body><h1>Hotel Alpenrose</h1></body>
        </html>
        """;

    private static final String JSON_LD_ARRAY = """
        <html>
        <head>
            <title>Test Page</title>
            <script type="application/ld+json">
            [
                {
                    "@context": "https://schema.org",
                    "@type": "WebPage",
                    "name": "Page"
                },
                {
                    "@context": "https://schema.org",
                    "@type": "LodgingBusiness",
                    "name": "Found in Array",
                    "address": {
                        "@type": "PostalAddress",
                        "addressLocality": "Berlin"
                    }
                }
            ]
            </script>
        </head>
        <body></body>
        </html>
        """;

    @Test
    void extractsLodgingBusinessFromJsonLd() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(JSON_LD_LODGING_BUSINESS, "https://www.huetten.com/chalet");

        assertThat(result).isPresent();
        final AccommodationImportResult data = result.get();
        assertThat(data.name()).isEqualTo("Chalet am Kogl");
        assertThat(data.address()).contains("Kogl 32");
        assertThat(data.address()).contains("8551");
        assertThat(data.address()).contains("Wies");
        assertThat(data.bookingUrl()).isEqualTo("https://www.huetten.com/chalet");
        assertThat(data.totalPrice()).isEqualByComparingTo(new BigDecimal("4025"));
        assertThat(data.rooms()).hasSize(3);
        assertThat(data.rooms().getFirst().name()).isEqualTo("Schlafzimmer 1");
        assertThat(data.rooms().getFirst().roomType()).isEqualTo(RoomType.DOUBLE);
        assertThat(data.rooms().getFirst().bedCount()).isEqualTo(2);
        assertThat(data.rooms().get(1).roomType()).isEqualTo(RoomType.SINGLE);
        assertThat(data.rooms().get(1).bedCount()).isEqualTo(1);
        assertThat(data.rooms().get(2).bedCount()).isEqualTo(4);
        assertThat(data.rooms().get(2).roomType()).isEqualTo(RoomType.QUAD);
    }

    @Test
    void extractsVacationRentalFromJsonLd() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(JSON_LD_VACATION_RENTAL, "https://airbnb.com/rooms/123");

        assertThat(result).isPresent();
        final AccommodationImportResult data = result.get();
        assertThat(data.name()).isEqualTo("Cozy Mountain Apartment");
        assertThat(data.address()).contains("Bergweg 5");
        assertThat(data.address()).contains("Innsbruck");
        assertThat(data.bookingUrl()).isEqualTo("https://airbnb.com/rooms/123");
    }

    @Test
    void fallsBackToOpenGraphMetaTags() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(OPEN_GRAPH_ONLY, "https://example.com/house");

        assertThat(result).isPresent();
        final AccommodationImportResult data = result.get();
        assertThat(data.name()).isEqualTo("Ferienhaus Waldblick");
        assertThat(data.notes()).contains("Schoenes Ferienhaus");
        assertThat(data.bookingUrl()).isEqualTo("https://example.com/house");
    }

    @Test
    void fallsBackToHtmlTitle() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(TITLE_ONLY, "https://example.com/place");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Some Vacation Place");
        assertThat(result.get().bookingUrl()).isEqualTo("https://example.com/place");
    }

    @Test
    void returnsEmptyForEmptyPage() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(EMPTY_PAGE, "https://example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void extractsNumericPriceFromPriceRange() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(JSON_LD_WITH_PRICE_NUMBER, "https://example.com/hotel");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Hotel Alpenrose");
        assertThat(result.get().totalPrice()).isEqualByComparingTo(new BigDecimal("2500"));
    }

    @Test
    void handlesJsonLdArray() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(JSON_LD_ARRAY, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Found in Array");
        assertThat(result.get().address()).contains("Berlin");
    }

    @Test
    void roomsDefaultToEmptyListWhenNoContainsPlace() {
        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(JSON_LD_VACATION_RENTAL, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().rooms()).isEmpty();
    }

    @Test
    void notesAreTruncatedTo500Chars() {
        final String longDescription = "A".repeat(600);
        final String html = """
            <html>
            <head>
                <title>Test</title>
                <meta property="og:title" content="Test Place">
                <meta property="og:description" content="%s">
            </head>
            <body></body>
            </html>
            """.formatted(longDescription);

        final HtmlAccommodationImportAdapter adapter = new HtmlAccommodationImportAdapter();

        final Optional<AccommodationImportResult> result = adapter.parseHtml(html, "https://example.com");

        assertThat(result).isPresent();
        assertThat(result.get().notes()).hasSize(500);
    }
}
