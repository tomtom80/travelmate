package de.evia.travelmate.expense.application;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.trip.TripProjection;

@Service
public class SettlementPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(SettlementPdfService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TemplateEngine templateEngine;

    public SettlementPdfService(final TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generatePdf(final ExpenseRepresentation expense,
                               final TripProjection projection,
                               final java.util.Map<java.util.UUID, String> participantNames,
                               final Locale locale) {
        final Context context = new Context(locale);
        context.setVariable("expense", expense);
        context.setVariable("participantNames", participantNames);
        context.setVariable("tripName", projection.tripName());
        context.setVariable("tripStartDate", projection.startDate() != null
            ? projection.startDate().format(DATE_FORMAT) : "—");
        context.setVariable("tripEndDate", projection.endDate() != null
            ? projection.endDate().format(DATE_FORMAT) : "—");
        context.setVariable("generatedDate", LocalDate.now().format(DATE_FORMAT));

        final String html = templateEngine.process("expense/settlement-pdf", context);

        try {
            final ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            renderer.createPDF(out);
            out.close();
            return out.toByteArray();
        } catch (final Exception e) {
            LOG.error("Failed to generate settlement PDF for trip {}", projection.tripId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
