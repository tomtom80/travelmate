package de.evia.travelmate.expense.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.application.representation.CategoryBreakdownRepresentation;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountEntryRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountRepresentation;
import de.evia.travelmate.expense.domain.expense.PartyAccountEntryType;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.trip.TripProjection;

class SettlementPdfServiceTest {

    private SettlementPdfService settlementPdfService;

    @BeforeEach
    void setUp() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setUseCodeAsDefaultMessage(true);

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);

        settlementPdfService = new SettlementPdfService(engine);
    }

    @Test
    void generatePdfRendersRealTemplateAndProducesValidBytes() {
        final UUID tripId = UUID.randomUUID();
        final TripProjection projection = TripProjection.create(
            tripId, new TenantId(UUID.randomUUID()), "Skiurlaub",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));

        final ExpenseRepresentation expense = new ExpenseRepresentation(
            UUID.randomUUID(), tripId, ExpenseStatus.SETTLED, false,
            List.of(), List.of(), Map.of(), List.of(),
            List.of(new CategoryBreakdownRepresentation(ExpenseCategory.GROCERIES, BigDecimal.TEN, new BigDecimal("100.0"), 1)),
            List.of(), List.of(), BigDecimal.TEN,
            List.of(new PartyAccountRepresentation(UUID.randomUUID(), "Familie A", List.of("Alice"),
                List.of(new PartyAccountEntryRepresentation(PartyAccountEntryType.ACCOMMODATION_SHARE, "Accommodation share", new BigDecimal("-10.00"), new BigDecimal("-10.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, new BigDecimal("-10.00"), BigDecimal.TEN, BigDecimal.ZERO)),
            List.of(), List.of(), List.of()
        );

        final byte[] pdf = settlementPdfService.generatePdf(expense, projection, Map.of(), Locale.GERMAN);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }
}
