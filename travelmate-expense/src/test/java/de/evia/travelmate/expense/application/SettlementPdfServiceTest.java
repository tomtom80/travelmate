package de.evia.travelmate.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.application.representation.AdvancePaymentRepresentation;
import de.evia.travelmate.expense.application.representation.CategoryBreakdownRepresentation;
import de.evia.travelmate.expense.application.representation.DailyCostRepresentation;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.application.representation.ParticipantSummaryRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountEntryRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountRepresentation;
import de.evia.travelmate.expense.application.representation.PartySettlementRepresentation;
import de.evia.travelmate.expense.application.representation.PartyTransferRepresentation;
import de.evia.travelmate.expense.application.representation.ReceiptRepresentation;
import de.evia.travelmate.expense.application.representation.TransferRepresentation;
import de.evia.travelmate.expense.application.representation.WeightingRepresentation;
import de.evia.travelmate.expense.domain.expense.PartyAccountEntryType;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.trip.TripProjection;

@ExtendWith(MockitoExtension.class)
class SettlementPdfServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private SettlementPdfService settlementPdfService;

    @Test
    void generatePdfRendersTemplateAndProducesBytes() {
        final UUID tripId = UUID.randomUUID();
        final TripProjection projection = TripProjection.create(
            tripId, new TenantId(UUID.randomUUID()), "Skiurlaub",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));

        final ExpenseRepresentation expense = new ExpenseRepresentation(
            UUID.randomUUID(), tripId, ExpenseStatus.SETTLED, false,
            List.of(), List.of(), Map.of(), List.of(),
            List.of(new CategoryBreakdownRepresentation(null, BigDecimal.TEN, new BigDecimal("100.0"), 1)),
            List.of(), List.of(), BigDecimal.TEN,
            List.of(new PartyAccountRepresentation(UUID.randomUUID(), "Familie A", List.of("Alice"),
                List.of(new PartyAccountEntryRepresentation(PartyAccountEntryType.ACCOMMODATION_SHARE, "Accommodation share", new BigDecimal("-10.00"), new BigDecimal("-10.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, new BigDecimal("-10.00"), BigDecimal.TEN, BigDecimal.ZERO)),
            List.of(), List.of(), List.of()
        );

        // Return valid XHTML that Flying Saucer can parse
        when(templateEngine.process(eq("expense/settlement-pdf"), any(IContext.class)))
            .thenReturn("<!DOCTYPE html><html><head><title>Test</title></head><body><p>Test PDF</p></body></html>");

        final byte[] pdf = settlementPdfService.generatePdf(expense, projection, java.util.Map.of(), Locale.GERMAN);

        assertThat(pdf).isNotEmpty();
        // PDF files start with %PDF
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }
}
