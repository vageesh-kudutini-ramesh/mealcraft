package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for meal plan preferences: week patterns and dietary rules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanPreferencesDTO {

    /** Enabled patterns: list of { key, label, dayOfWeek (1=Mon), mealType, dietFilter }. */
    private List<Map<String, Object>> patterns;

    /** Dietary rules: noGluten, minVegetarianDinnersPerWeek, maxCaloriesPerDinner. */
    private Map<String, Object> dietaryRules;
}
