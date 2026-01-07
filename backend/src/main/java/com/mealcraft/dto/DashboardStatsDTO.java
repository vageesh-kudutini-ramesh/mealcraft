package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for dashboard statistics and alerts
 * 
 * Used to display comprehensive dashboard information including:
 * - Expiring items alerts
 * - Expired items alerts
 * - Low stock alerts
 * - Quick statistics
 * - Recent saved recipes
 * - Today's meal plan
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    /**
     * Total number of pantry items
     */
    private Long totalPantryItems;

    /**
     * Number of items expiring soon (1-5 days)
     */
    private Long expiringSoonCount;

    /**
     * Number of expired items
     */
    private Long expiredCount;

    /**
     * Number of low-stock items
     */
    private Long lowStockCount;

    /**
     * List of items expiring soon (for alert panel)
     */
    private List<PantryItemDTO> expiringSoonItems;

    /**
     * List of expired items (for alert panel)
     */
    private List<PantryItemDTO> expiredItems;

    /**
     * List of low-stock items (for alert panel)
     */
    private List<PantryItemDTO> lowStockItems;

    /**
     * Total number of saved recipes
     */
    private Long savedRecipesCount;

    /**
     * Recent saved recipes (for dashboard display)
     */
    private List<RecipeDTO> recentRecipes;

    /**
     * Today's meal plan (if any)
     */
    private List<MealPlanDTO> todaysMealPlan;
}




