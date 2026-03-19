package de.evia.travelmate.expense.adapters.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;

@ExtendWith(MockitoExtension.class)
class OllamaReceiptScanAdapterTest {

    @Mock
    private OllamaClient ollamaClient;

    private OllamaReceiptScanAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OllamaReceiptScanAdapter(ollamaClient);
    }

    @Test
    void parsesCompleteReceiptResponse() {
        final String json = """
            {
                "totalAmount": 23.45,
                "date": "2025-03-15",
                "storeName": "REWE",
                "category": "GROCERIES",
                "lineItems": [
                    {"name": "Milch", "price": 1.29},
                    {"name": "Brot", "price": 2.49},
                    {"name": "Butter", "price": 1.99}
                ]
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("23.45"));
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(result.storeName()).isEqualTo("REWE");
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(result.lineItems()).hasSize(3);
        assertThat(result.lineItems().getFirst().name()).isEqualTo("Milch");
        assertThat(result.lineItems().getFirst().amount()).isEqualByComparingTo(new BigDecimal("1.29"));
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void parsesMinimalReceiptResponse() {
        final String json = """
            {
                "totalAmount": 42.00,
                "date": null,
                "storeName": null,
                "category": null,
                "lineItems": []
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("42.00"));
        assertThat(result.receiptDate()).isNull();
        assertThat(result.storeName()).isNull();
        assertThat(result.suggestedCategory()).isNull();
        assertThat(result.lineItems()).isEmpty();
    }

    @Test
    void handlesInvalidDateGracefully() {
        final String json = """
            {
                "totalAmount": 10.00,
                "date": "invalid-date",
                "storeName": "Shop"
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.receiptDate()).isNull();
    }

    @Test
    void handlesUnknownCategoryGracefully() {
        final String json = """
            {
                "totalAmount": 10.00,
                "category": "UNKNOWN_CATEGORY"
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void handlesInvalidJsonGracefully() {
        final String invalidJson = "this is not json at all";

        final ReceiptScanResult result = adapter.parseResponse(invalidJson);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void scanReturnsFailureWhenOllamaNotAvailable() {
        when(ollamaClient.isAvailable()).thenReturn(false);

        final ReceiptScanResult result = adapter.scan(new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("nicht verfuegbar");
    }

    @Test
    void scanReturnsFailureWhenOllamaReturnsEmpty() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.generateWithImage(anyString(), any(byte[].class))).thenReturn(Optional.empty());

        final ReceiptScanResult result = adapter.scan(new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("fehlgeschlagen");
    }

    @Test
    void scanReturnsSuccessfulResultFromOllama() {
        final String json = """
            {"totalAmount": 15.99, "storeName": "Aldi", "category": "GROCERIES", "lineItems": []}
            """;
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.generateWithImage(anyString(), any(byte[].class))).thenReturn(Optional.of(json));

        final ReceiptScanResult result = adapter.scan(new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
        assertThat(result.storeName()).isEqualTo("Aldi");
    }

    @Test
    void scanReturnsFailureOnMalformedOllamaResponse() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.generateWithImage(anyString(), any(byte[].class)))
            .thenReturn(Optional.of("not valid json {{{"));

        final ReceiptScanResult result = adapter.scan(new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void skipsLineItemsWithMissingNameOrPrice() {
        final String json = """
            {
                "totalAmount": 10.00,
                "lineItems": [
                    {"name": "Valid Item", "price": 5.00},
                    {"name": null, "price": 3.00},
                    {"name": "No Price", "price": null},
                    {"name": "Another Valid", "price": 2.00}
                ]
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.lineItems()).hasSize(2);
        assertThat(result.lineItems().get(0).name()).isEqualTo("Valid Item");
        assertThat(result.lineItems().get(1).name()).isEqualTo("Another Valid");
    }

    @Test
    void handlesPriceAsString() {
        final String json = """
            {
                "totalAmount": "23,45",
                "lineItems": []
            }
            """;

        final ReceiptScanResult result = adapter.parseResponse(json);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("23.45"));
    }

    @Test
    void parsesAllExpenseCategories() {
        for (final ExpenseCategory category : ExpenseCategory.values()) {
            final String json = String.format("""
                {"totalAmount": 10.00, "category": "%s"}
                """, category.name());

            final ReceiptScanResult result = adapter.parseResponse(json);

            assertThat(result.suggestedCategory()).isEqualTo(category);
        }
    }
}
