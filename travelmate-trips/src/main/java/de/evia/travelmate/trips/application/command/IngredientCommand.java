package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;

public record IngredientCommand(
    String name,
    BigDecimal quantity,
    String unit
) {
}
