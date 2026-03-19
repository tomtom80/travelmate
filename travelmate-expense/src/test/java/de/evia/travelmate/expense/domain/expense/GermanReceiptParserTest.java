package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class GermanReceiptParserTest {

    @Test
    void parsesReweLeergutbon() {
        final String text = """
            LEERGUTBON
            0840933
            REWE
            Bahnhofstrasse 2
            88273 Fronreute
            Mehrweg:
            1x Kasten a 1,50    1,50 EUR
            20x Flasche a 0,08  1,60 EUR
            Summe:              3,10 EUR
            Einweg:
            53x Einweg a 0,25   13,25 EUR
            Summe:              13,25 EUR
            Summe:              16,35 EUR
            23.08.2022 11:19:05
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("16.35");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2022, 8, 23));
        assertThat(result.storeName()).isEqualTo("REWE");
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(result.rawText()).isNotBlank();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void parsesTankstelleReceipt() {
        final String text = """
            MTB - Tankstelle
            Wiesenstrasse 2
            71364 Winnenden
            SUPER E10 / ZP3
            25,03 Liter EUR/Liter 1,599  40,02
            Endsumme EUR   40,02
            02.09.2024 20:03
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("40.02");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2024, 9, 2));
        assertThat(result.storeName()).isEqualTo("MTB - Tankstelle");
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.FUEL);
    }

    @Test
    void parsesSpielwarenReceiptWithGesamtbetrag() {
        final String text = """
            E.+E. Spielwaren GmbH
            Hauptstrasse 15
            88069 Tettnang
            1x Lego Set             16,99
            Gesamtbetrag/EUR        16,99
            16.01.2026 14:32
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("16.99");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2026, 1, 16));
        assertThat(result.storeName()).isEqualTo("E.+E. Spielwaren GmbH");
    }

    @Test
    void parsesKikReceiptWithSumme() {
        final String text = """
            KiK Filiale 8168
            Kinder-Hose        5,99
            Boxershorts         3,99
            T-Shirt             4,99
            Socken 3er          2,99
            Kinder-Hose         5,99
            Jogginghose         7,99
            Pullover            8,99
            Muetze              3,99
            Handschuhe          4,99
            Schal               4,99
            Summe              58,90
            02.09.2024 15:30
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("58.90");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2024, 9, 2));
        assertThat(result.storeName()).isEqualTo("KiK Filiale 8168");
    }

    @Test
    void parsesReceiptWithShortDateFormat() {
        final String text = """
            Baeckerei Schulz
            3x Broetchen   1,50
            Summe           1,50
            23.08.22 08:15
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("1.50");
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2022, 8, 23));
    }

    @Test
    void parsesReceiptWithZuZahlen() {
        final String text = """
            Gasthaus Adler
            Schnitzel          12,90
            Pommes              3,50
            Apfelsaft           2,80
            Zu zahlen          19,20
            10.03.2025 19:45
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("19.20");
    }

    @Test
    void returnsFailureForNullText() {
        final ReceiptScanResult result = GermanReceiptParser.parse(null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void returnsFailureForBlankText() {
        final ReceiptScanResult result = GermanReceiptParser.parse("   ");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void returnsFailureForUnreadableText() {
        final ReceiptScanResult result = GermanReceiptParser.parse("///...///");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("manuell");
    }

    @Test
    void guessesGroceryCategoryForEdeka() {
        final String text = """
            EDEKA Neukauf
            Butter              2,49
            Summe               2,49
            01.01.2025
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.GROCERIES);
    }

    @Test
    void returnsPartialResultWhenOnlyAmountFound() {
        final String text = """
            Summe  42,00
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("42.00");
        assertThat(result.receiptDate()).isNull();
    }

    @Test
    void returnsPartialResultWhenOnlyDateFound() {
        final String text = """
            Some unreadable receipt
            15.06.2025
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isNull();
        assertThat(result.receiptDate()).isEqualTo(LocalDate.of(2025, 6, 15));
    }

    @Test
    void parsesReceiptWithDotDecimalSeparator() {
        final String text = """
            Shell Station
            Diesel           65.30
            Total            65.30
            05.11.2024
            """;

        final ReceiptScanResult result = GermanReceiptParser.parse(text);

        assertThat(result.success()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("65.30");
        assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.FUEL);
    }
}
