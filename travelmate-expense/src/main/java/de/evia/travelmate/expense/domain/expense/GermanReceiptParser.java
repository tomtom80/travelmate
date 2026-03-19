package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service that parses raw OCR text from German receipts.
 * Extracts total amount, date, and store name using regex patterns.
 * Pure domain logic — no framework dependencies.
 */
public final class GermanReceiptParser {

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "(?i)(?:summe|gesamtbetrag|endsumme|total|zu\\s+zahlen|betrag|gesamt)" +
        "\\s*[:/]?\\s*(?:EUR)?\\s*(-?\\d{1,6}[,.]\\d{2})");

    private static final Pattern STANDALONE_TOTAL_PATTERN = Pattern.compile(
        "(?i)(?:summe|gesamtbetrag|endsumme|total|zu\\s+zahlen|betrag|gesamt)" +
        "\\s*(?:EUR)?\\s*$",
        Pattern.MULTILINE);

    private static final Pattern AMOUNT_ON_LINE_PATTERN = Pattern.compile(
        "(-?\\d{1,6}[,.]\\d{2})\\s*$");

    private static final Pattern DATE_LONG_PATTERN = Pattern.compile(
        "(\\d{2}\\.\\d{2}\\.\\d{4})");

    private static final Pattern DATE_SHORT_PATTERN = Pattern.compile(
        "(\\d{2}\\.\\d{2}\\.\\d{2})(?!\\d)");

    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy");

    private static final List<String> KNOWN_STORES = List.of(
        "REWE", "EDEKA", "ALDI", "LIDL", "PENNY", "NETTO", "NORMA", "KAUFLAND",
        "DM", "ROSSMANN", "MUELLER", "KIK", "SPAR", "HOFER", "BILLA", "MIGROS",
        "COOP", "DENNER", "SHELL", "ARAL", "OMV", "JET", "ESSO"
    );

    private GermanReceiptParser() {
    }

    /**
     * Parses raw OCR text into a ReceiptScanResult.
     *
     * @param rawText the raw OCR text output
     * @return a ReceiptScanResult with extracted fields, or failure if text is blank
     */
    public static ReceiptScanResult parse(final String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return ReceiptScanResult.failure("Kein Text erkannt.");
        }

        final BigDecimal totalAmount = extractTotalAmount(rawText);
        final LocalDate receiptDate = extractDate(rawText);
        final String storeName = extractStoreName(rawText);
        final ExpenseCategory suggestedCategory = CategoryGuesser.guess(storeName);

        if (totalAmount == null && receiptDate == null && storeName == null) {
            return ReceiptScanResult.failure("Beleg konnte nicht gelesen werden. Bitte manuell eingeben.");
        }

        return new ReceiptScanResult(
            true,
            totalAmount,
            receiptDate,
            storeName,
            suggestedCategory,
            List.of(),
            rawText,
            null
        );
    }

    private static BigDecimal extractTotalAmount(final String text) {
        final List<BigDecimal> candidates = new ArrayList<>();

        // Strategy 1: Total keyword followed by amount on the same line
        final Matcher totalMatcher = TOTAL_PATTERN.matcher(text);
        while (totalMatcher.find()) {
            final BigDecimal amount = parseGermanNumber(totalMatcher.group(1));
            if (amount != null) {
                candidates.add(amount);
            }
        }

        // Strategy 2: Total keyword on a line, amount on the same line but separated by spaces
        final String[] lines = text.split("\\r?\\n");
        for (final String line : lines) {
            final Matcher keywordMatcher = STANDALONE_TOTAL_PATTERN.matcher(line);
            if (keywordMatcher.find()) {
                // Look for amount earlier on same line
                final Matcher amountMatcher = AMOUNT_ON_LINE_PATTERN.matcher(line.substring(0, keywordMatcher.start()));
                if (amountMatcher.find()) {
                    final BigDecimal amount = parseGermanNumber(amountMatcher.group(1));
                    if (amount != null) {
                        candidates.add(amount);
                    }
                }
            }
        }

        // Return the last (typically largest / final total) candidate
        if (!candidates.isEmpty()) {
            return candidates.getLast();
        }
        return null;
    }

    private static LocalDate extractDate(final String text) {
        // Try long format first (dd.MM.yyyy)
        final Matcher longMatcher = DATE_LONG_PATTERN.matcher(text);
        if (longMatcher.find()) {
            try {
                return LocalDate.parse(longMatcher.group(1), LONG_DATE_FORMAT);
            } catch (final DateTimeParseException ignored) {
                // fall through to short format
            }
        }

        // Try short format (dd.MM.yy)
        final Matcher shortMatcher = DATE_SHORT_PATTERN.matcher(text);
        if (shortMatcher.find()) {
            try {
                return LocalDate.parse(shortMatcher.group(1), SHORT_DATE_FORMAT);
            } catch (final DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String extractStoreName(final String text) {
        final String[] lines = text.split("\\r?\\n");

        // Strategy 1: Look for known store names anywhere in text
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (final String store : KNOWN_STORES) {
                if (trimmed.toUpperCase().contains(store)) {
                    return trimmed;
                }
            }
        }

        // Strategy 2: First non-empty line that looks like a name (not a number, not too short)
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.length() >= 3 && !trimmed.matches("^[\\d\\s.,/:-]+$")) {
                return trimmed;
            }
        }

        return null;
    }

    private static BigDecimal parseGermanNumber(final String numberStr) {
        if (numberStr == null) {
            return null;
        }
        try {
            // German format uses comma as decimal separator
            final String normalized = numberStr.replace(',', '.');
            return new BigDecimal(normalized);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
