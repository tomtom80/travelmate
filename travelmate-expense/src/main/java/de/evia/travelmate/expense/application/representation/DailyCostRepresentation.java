package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCostRepresentation(LocalDate date, BigDecimal total, int receiptCount) {
}
