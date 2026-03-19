package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceiptScanResult(
    boolean success,
    BigDecimal totalAmount,
    LocalDate receiptDate,
    String storeName,
    ExpenseCategory suggestedCategory,
    List<ScannedLineItem> lineItems,
    String rawText,
    String errorMessage
) {

    public static ReceiptScanResult empty() {
        return new ReceiptScanResult(false, null, null, null, null, List.of(), null,
            "OCR not available. Please enter receipt data manually.");
    }

    public static ReceiptScanResult failure(final String errorMessage) {
        return new ReceiptScanResult(false, null, null, null, null, List.of(), null, errorMessage);
    }
}
