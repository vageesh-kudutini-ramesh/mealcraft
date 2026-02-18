package com.mealcraft.service;

import com.mealcraft.dto.PantryItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Optimized ingredient name matching for pantry-to-recipe deduction.
 * Uses normalization, fuzzy matching, and unit conversion for accurate subtraction.
 */
@Service
public class IngredientMatchingService {

    private static final Pattern PAREN_PATTERN = Pattern.compile("\\s*\\([^)]*\\)\\s*");
    private static final Set<String> DESCRIPTOR_WORDS = new HashSet<>(Arrays.asList(
            "fresh", "diced", "chopped", "grated", "minced", "organic", "extra", "virgin",
            "raw", "cooked", "frozen", "canned", "dried", "sliced", "crushed", "whole",
            "ground", "optional", "large", "small", "medium", "fine", "coarse"
    ));

    @Autowired
    private UnitConversionService unitConversionService;

    /**
     * Normalizes an ingredient name for matching.
     * Removes descriptors, parenthetical content, and normalizes plural/singular.
     */
    public String normalizeIngredientName(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String n = name.toLowerCase().trim();
        n = PAREN_PATTERN.matcher(n).replaceAll(" ");
        n = n.replaceAll("\\s+", " ").trim();
        String[] tokens = n.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (t.isEmpty() || DESCRIPTOR_WORDS.contains(t)) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(toSingular(t));
        }
        return sb.toString().trim();
    }

    private static String toSingular(String word) {
        if (word.length() < 3) return word;
        if (word.endsWith("sses")) return word.substring(0, word.length() - 2); // glasses->glass
        if (word.endsWith("ies") && word.length() > 4) return word.substring(0, word.length() - 3) + "y"; // berries->berry
        if (word.endsWith("es") && !word.endsWith("ss")) return word.substring(0, word.length() - 2); // tomatoes->tomato
        if (word.endsWith("s") && !word.endsWith("ss") && !word.equals("rice") && !word.equals("juice"))
            return word.substring(0, word.length() - 1); // eggs->egg
        return word;
    }

    /**
     * Checks if recipe ingredient name matches pantry item name.
     * Uses: exact match, substring containment, or word-overlap (Jaccard-style).
     */
    public boolean namesMatch(String recipeName, String pantryName) {
        String r = normalizeIngredientName(recipeName);
        String p = normalizeIngredientName(pantryName);
        if (r.isEmpty() || p.isEmpty()) return false;
        if (r.equals(p)) return true;
        if (r.contains(p) || p.contains(r)) return true;
        Set<String> rWords = Arrays.stream(r.split("\\s+")).filter(w -> w.length() > 1).collect(Collectors.toSet());
        Set<String> pWords = Arrays.stream(p.split("\\s+")).filter(w -> w.length() > 1).collect(Collectors.toSet());
        if (rWords.isEmpty() || pWords.isEmpty()) return false;
        Set<String> intersection = new HashSet<>(rWords);
        intersection.retainAll(pWords);
        double jaccard = (double) intersection.size() / (rWords.size() + pWords.size() - intersection.size());
        return jaccard >= 0.5;
    }

    /**
     * Gets total available quantity of an ingredient from pantry, converted to recipe's unit.
     * Sums all matching pantry items (by name) after converting units.
     */
    public double getAvailableInUnit(List<PantryItemDTO> pantryItems, String ingredientName, String targetUnit) {
        if (pantryItems == null || targetUnit == null) return 0;
        double total = 0;
        String normTarget = unitConversionService.normalizeUnit(targetUnit);
        for (PantryItemDTO item : pantryItems) {
            if (item == null || item.getItemName() == null || item.getUnit() == null || item.getQuantity() == null)
                continue;
            if (!namesMatch(ingredientName, item.getItemName())) continue;
            Double converted = unitConversionService.convertQuantity(item.getQuantity(), item.getUnit(), targetUnit);
            if (converted != null && converted > 0) {
                total += converted;
            } else if (unitConversionService.normalizeUnit(item.getUnit()).equals(normTarget)) {
                total += item.getQuantity();
            }
        }
        return total;
    }
}
