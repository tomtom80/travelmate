package de.evia.travelmate.expense.adapters.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.expense.domain.expense.GermanReceiptParser;
import de.evia.travelmate.expense.domain.expense.ReceiptScanPort;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Receipt OCR adapter using Tesseract via tess4j.
 * Performs OCR on receipt images and delegates text parsing to {@link GermanReceiptParser}.
 * Gracefully degrades if Tesseract native libraries are not available.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "travelmate.llm.enabled", havingValue = "false", matchIfMissing = true)
public class TesseractReceiptScanAdapter implements ReceiptScanPort {

    private static final Logger LOG = LoggerFactory.getLogger(TesseractReceiptScanAdapter.class);

    private final String tessdataPath;
    private final String language;
    private final boolean available;

    public TesseractReceiptScanAdapter(
        @Value("${travelmate.ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}") final String tessdataPath,
        @Value("${travelmate.ocr.language:deu}") final String language
    ) {
        this.tessdataPath = tessdataPath;
        this.language = language;
        this.available = checkTesseractAvailable();
    }

    @Override
    public ReceiptScanResult scan(final byte[] imageData, final String mimeType) {
        if (!available) {
            return ReceiptScanResult.failure(
                "Tesseract OCR ist nicht installiert. Bitte Beleg manuell eingeben.");
        }

        try {
            final BufferedImage image = readImage(imageData);
            if (image == null) {
                return ReceiptScanResult.failure("Bild konnte nicht gelesen werden.");
            }

            final Tesseract tesseract = createTesseract();
            final String rawText = tesseract.doOCR(image);

            LOG.debug("OCR raw text:\n{}", rawText);

            return GermanReceiptParser.parse(rawText);
        } catch (final TesseractException e) {
            LOG.warn("Tesseract OCR failed: {}", e.getMessage());
            return ReceiptScanResult.failure("OCR-Fehler: " + e.getMessage());
        }
    }

    private Tesseract createTesseract() {
        final Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(6); // Assume uniform block of text
        return tesseract;
    }

    private BufferedImage readImage(final byte[] imageData) {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(bais);
        } catch (final IOException e) {
            LOG.warn("Failed to read image: {}", e.getMessage());
            return null;
        }
    }

    private boolean checkTesseractAvailable() {
        try {
            final Tesseract tesseract = createTesseract();
            // Try to initialize — will throw if native libs or tessdata missing
            tesseract.doOCR(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY));
            return true;
        } catch (final TesseractException e) {
            // Tesseract initialized but couldn't process empty image — that's fine, it's available
            return true;
        } catch (final UnsatisfiedLinkError | NoClassDefFoundError | Exception e) {
            LOG.warn("Tesseract OCR is not available: {}. Receipt scanning will be disabled.", e.getMessage());
            return false;
        }
    }
}
