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
}




