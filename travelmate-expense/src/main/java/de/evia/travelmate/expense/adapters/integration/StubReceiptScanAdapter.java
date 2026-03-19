package de.evia.travelmate.expense.adapters.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.expense.domain.expense.ReceiptScanPort;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;

/**
 * Stub adapter that always returns an empty result.
 * Used in test profile and as MVP fallback when no OCR engine is available.
 */
@Component
@Profile("test")
public class StubReceiptScanAdapter implements ReceiptScanPort {

    @Override
    public ReceiptScanResult scan(final byte[] imageData, final String mimeType) {
        return ReceiptScanResult.empty();
    }
}
