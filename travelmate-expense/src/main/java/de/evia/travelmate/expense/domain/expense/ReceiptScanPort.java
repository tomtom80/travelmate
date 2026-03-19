package de.evia.travelmate.expense.domain.expense;

/**
 * Secondary port for receipt OCR scanning.
 * Implementations live in adapters/integration/.
 */
public interface ReceiptScanPort {

    ReceiptScanResult scan(byte[] imageData, String mimeType);
}
