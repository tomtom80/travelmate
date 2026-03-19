package de.evia.travelmate.expense.domain.expense;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service that guesses an expense category from a store name.
 * Uses keyword matching — no framework dependencies.
 * LinkedHashMap guarantees evaluation order: more specific categories first.
 */
public final class CategoryGuesser {

    private static final Map<ExpenseCategory, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(ExpenseCategory.GROCERIES, List.of(
            "edeka", "rewe", "aldi", "lidl", "spar", "penny", "netto", "norma",
            "kaufland", "hofer", "billa", "migros", "coop", "denner",
            "baeckerei", "brot", "backhaus", "backwerk", "baecker"
        ));
        KEYWORDS.put(ExpenseCategory.RESTAURANT, List.of(
            "restaurant", "gasthaus", "pizzeria", "trattoria", "bistro",
            "cafe", "kaffee", "starbucks", "mcdonalds", "burger king",
            "kebab", "doener", "wirtshaus", "gasthof"
        ));
        KEYWORDS.put(ExpenseCategory.FUEL, List.of(
            "shell", "aral", "total", "omv", "tankstelle", "esso", "jet",
            "bp", "agip", "eni", "avanti"
        ));
        KEYWORDS.put(ExpenseCategory.ACTIVITY, List.of(
            "bergbahn", "seilbahn", "skilift", "therme", "museum",
            "schwimmbad", "kino", "freizeitpark", "zoo", "aquapark",
            "skipass", "lift"
        ));
        KEYWORDS.put(ExpenseCategory.TRANSPORT, List.of(
            "db", "bahn", "bus", "taxi", "uber", "maut", "parkhaus",
            "parking", "flixbus", "parkgebuehr"
        ));
        KEYWORDS.put(ExpenseCategory.HEALTH, List.of(
            "apotheke", "drogerie", "dm", "mueller", "rossmann"
        ));
    }

    private CategoryGuesser() {
    }

    public static ExpenseCategory guess(final String storeName) {
        if (storeName == null || storeName.isBlank()) {
            return ExpenseCategory.OTHER;
        }
        final String lower = storeName.toLowerCase();
        for (final Map.Entry<ExpenseCategory, List<String>> entry : KEYWORDS.entrySet()) {
            for (final String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return ExpenseCategory.OTHER;
    }
}
