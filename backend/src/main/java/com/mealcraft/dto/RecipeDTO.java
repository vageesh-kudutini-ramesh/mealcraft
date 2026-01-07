package com.mealcraft.dto;

import com.mealcraft.model.SavedRecipe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for recipe information
 * 
 * Used for displaying recipe details from suggestions and saved recipes.
 * Contains complete recipe information including ingredients and instructions.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDTO {

    /**
     * Recipe ID (null for suggested recipes, set for saved recipes)
     */
    private Long id;

    /**
     * Recipe name/title
     */
    private String recipeName;

    /**
     * Meal type (Breakfast, Lunch, Dinner, All)
     */
    private SavedRecipe.MealType mealType;

    /**
     * Recipe image URL
     */
    private String imageUrl;

    /**
     * Estimated preparation time in minutes
     */
    private Integer prepTimeMinutes;

    /**
     * Estimated cooking time in minutes
     */
    private Integer cookTimeMinutes;

    /**
     * Number of servings
     */
    private Integer servings;

    /**
     * Ingredients list (parsed from JSON)
     * Format: List of maps with "name", "quantity", "unit"
     */
    private List<Map<String, Object>> ingredients;

    /**
     * Step-by-step cooking instructions
     */
    private String instructions;

    /**
     * User's custom notes (for saved recipes only)
     */
    private String notes;

    /**
     * Match percentage (how many ingredients user has available)
     * Used for recipe suggestions
     */
    private Double matchPercentage;

    /**
     * External recipe ID from Spoonacular API (if applicable)
     */
    private Long externalRecipeId;

    /**
     * List of expiring ingredients this recipe uses (for priority display)
     * Format: List of ingredient names with days until expiry
     */
    private List<Map<String, Object>> expiringIngredients;

    /**
     * Whether this recipe uses expiring ingredients (for priority indicator)
     */
    private Boolean usesExpiringIngredients = false;
}




