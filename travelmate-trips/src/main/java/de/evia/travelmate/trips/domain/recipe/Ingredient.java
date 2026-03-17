package de.evia.travelmate.trips.domain.recipe;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;

public record Ingredient(String name, BigDecimal quantity, String unit) {

    public Ingredient {
        argumentIsNotBlank(name, "ingredient name");
        argumentIsNotNull(quantity, "quantity");
        argumentIsTrue(quantity.compareTo(BigDecimal.ZERO) > 0,
            "Quantity must be greater than zero.");
        argumentIsNotBlank(unit, "unit");
    }
}
