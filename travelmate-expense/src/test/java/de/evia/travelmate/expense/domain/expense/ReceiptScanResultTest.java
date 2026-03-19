package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReceiptScanResultTest {

    @Test
    void emptyResultIndicatesFailure() {
        final ReceiptScanResult result = ReceiptScanResult.empty();

        assertThat(result.success()).isFalse();
        assertThat(result.totalAmount()).isNull();
        assertThat(result.receiptDate()).isNull();
        assertThat(result.storeName()).isNull();
        assertThat(result.lineItems()).isEmpty();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void failureResultContainsErrorMessage() {
        final ReceiptScanResult result = ReceiptScanResult.failure("Image too dark");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Image too dark");
    }

    @Test
    void successfulResultContainsExtractedData() {
        final ReceiptScanResult result = new ReceiptScanResult(
            true,
            new BigDecimal("47.83"),
            LocalDate.of(2026, 7, 18),
            "EDEKA Markt Mueller",
            ExpenseCategory.GROCERIES,
            List.of(new ScannedLineItem("Bio-Milch 1L", new BigDecimal("1.89"))),
            "raw ocr text",
            null
        );

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("47.83");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(result.storeName()).isEqualTo("EDEKA Markt Mueller");
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(result.lineItems()).hasSize(1);
    }
}
