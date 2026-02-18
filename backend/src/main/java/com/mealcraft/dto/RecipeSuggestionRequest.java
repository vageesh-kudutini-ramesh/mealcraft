package com.mealcraft.dto;

import com.mealcraft.model.SavedRecipe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for recipe suggestion requests
 * 
 * Used when user requests recipe suggestions based on pantry ingredients.
 * Contains meal type filter for targeted suggestions.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSuggestionRequest {

    /**
     * Meal type filter (Breakfast, Lunch, Dinner, All)
     * Optional - defaults to "All" if not specified
     */
    private SavedRecipe.MealType mealType = SavedRecipe.MealType.ALL;

    /**
     * Cuisine/region filter from TheMealDB (e.g. Indian, American, Italian).
     * Optional - null or empty means all cuisines.
     */
    private String area;

    /**
     * Diet filter: "ALL", "VEGETARIAN", "NON_VEGETARIAN".
     * Optional - defaults to "ALL".
     */
    private String diet = "ALL";

    /**
     * User dietary rules from Meal Plan preferences.
     * Keys: NO_GLUTEN, NO_DAIRY, MIN_VEGETARIAN_DINNERS, VEGAN_ONLY, MAX_CALORIES_PER_DINNER, MAX_MEAT_DINNERS.
     * Applied when filtering suggested recipes.
     */
    private java.util.Map<String, Object> dietaryRules;

    /**
     * Offset for pagination when refreshing (e.g. 0, 15, 30). Used to get different results on each refresh.
     */
    private Integer offset;
}




