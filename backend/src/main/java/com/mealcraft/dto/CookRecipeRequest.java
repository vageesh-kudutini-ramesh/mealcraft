package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for cooking a recipe request
 * 
 * Used when user wants to cook a recipe with adjusted ingredient quantities.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CookRecipeRequest {
    
    /**
     * Recipe ID (external recipe ID for suggested recipes, or saved recipe ID)
     */
    private Long recipeId;
    
    /**
     * Adjusted ingredients with quantities
     * Format: Map of ingredient name to adjusted quantity
     */
    private Map<String, Double> adjustedIngredients;
}
