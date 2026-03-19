package de.evia.travelmate.expense.adapters.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.expense.domain.expense.ReceiptScanPort;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;

/**
 * No-op production adapter that returns empty results.
 * The user can still photograph receipts and use them as visual reference while entering data manually.
 * A real OCR adapter (Tesseract or Cloud Vision) can replace this as a drop-in when configured.
 */
@Component
@Profile("!test")
public class NoOpReceiptScanAdapter implements ReceiptScanPort {

    @Override
    public ReceiptScanResult scan(final byte[] imageData, final String mimeType) {
        return ReceiptScanResult.empty();
    }
}
