package com.mealcraft.dto;

import com.mealcraft.model.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for meal plan information
 * 
 * Used for displaying and managing meal plans in the weekly calendar.
 * Contains recipe snapshot data (retained even if saved recipe is deleted).
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDTO {

    /**
     * Meal plan ID
     */
    private Long id;

    /**
     * Date for which the meal is planned
     */
    private LocalDate date;

    /**
     * Meal type (Breakfast, Lunch, Dinner)
     */
    private MealPlan.MealType mealType;

    /**
     * Recipe name (snapshot)
     */
    private String recipeName;

    /**
     * Ingredients list (parsed from JSON, snapshot)
     */
    private List<Map<String, Object>> ingredients;

    /**
     * Step-by-step cooking instructions (snapshot)
     */
    private String instructions;

    /**
     * Recipe image URL (snapshot)
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
     * ID of the saved recipe this meal plan was created from (if applicable)
     * Can be null for quick-add recipes
     */
    private Long savedRecipeId;
}




