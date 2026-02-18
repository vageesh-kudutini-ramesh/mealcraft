package com.mealcraft.service;

import com.mealcraft.dto.DashboardStatsDTO;
import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.dto.PantryItemDTO;
import com.mealcraft.dto.RecipeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard Service
 * 
 * Aggregates data from multiple services to provide comprehensive dashboard statistics.
 * Includes expiring items, expired items, low-stock alerts, and recent recipes.
 * 
 * @author MealCraft Team
 */
@Service
public class DashboardService {

    @Autowired
    private PantryItemService pantryItemService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private MealPlanService mealPlanService;

    /**
     * Gets comprehensive dashboard statistics for a user
     * 
     * @param userId User's ID
     * @return DashboardStatsDTO with all dashboard information
     */
    public DashboardStatsDTO getDashboardStats(Long userId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // Get pantry statistics
        List<PantryItemDTO> allPantryItems = pantryItemService.getAllPantryItems(userId);
        stats.setTotalPantryItems((long) allPantryItems.size());

        // Get expiring items (within 7 days)
        List<PantryItemDTO> expiringItems = pantryItemService.getExpiringItems(userId, null);
        stats.setExpiringSoonCount((long) expiringItems.size());
        stats.setExpiringSoonItems(expiringItems);

        // Get expired items
        List<PantryItemDTO> expiredItems = pantryItemService.getExpiredItems(userId, null);
        stats.setExpiredCount((long) expiredItems.size());
        stats.setExpiredItems(expiredItems);

        // Get low-stock items
        List<PantryItemDTO> lowStockItems = pantryItemService.getLowStockItems(userId);
        stats.setLowStockCount((long) lowStockItems.size());
        stats.setLowStockItems(lowStockItems);

        // Get saved recipes count and recent recipes
        List<RecipeDTO> savedRecipes = recipeService.getSavedRecipes(userId);
        stats.setSavedRecipesCount((long) savedRecipes.size());
        
        // Get recent recipes (last 5)
        List<RecipeDTO> recentRecipes = savedRecipes.stream()
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.setRecentRecipes(recentRecipes);

        // Get today's meal plan
        LocalDate today = LocalDate.now();
        List<MealPlanDTO> todaysMealPlan = mealPlanService.getMealPlanByDate(userId, today);
        stats.setTodaysMealPlan(todaysMealPlan);

        return stats;
    }
}

