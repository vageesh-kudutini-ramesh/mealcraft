package com.mealcraft.repository;

import com.mealcraft.model.SavedRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for SavedRecipe entity
 * 
 * Provides CRUD operations and custom query methods for saved recipe management.
 * Includes search and filter capabilities.
 * 
 * @author MealCraft Team
 */
@Repository
public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {

    /**
     * Finds all saved recipes for a specific user
     * 
     * @param userId User's ID
     * @return List of saved recipes belonging to the user
     */
    List<SavedRecipe> findByUserId(Long userId);

    /**
     * Searches saved recipes by name (case-insensitive partial match)
     * 
     * @param userId User's ID
     * @param recipeName Recipe name to search (partial match)
     * @return List of matching saved recipes
     */
    @Query("SELECT r FROM SavedRecipe r WHERE r.user.id = :userId " +
           "AND LOWER(r.recipeName) LIKE LOWER(CONCAT('%', :recipeName, '%'))")
    List<SavedRecipe> searchByRecipeName(@Param("userId") Long userId,
                                         @Param("recipeName") String recipeName);

    /**
     * Finds saved recipes by meal type for a specific user
     * 
     * @param userId User's ID
     * @param mealType Meal type to filter by
     * @return List of saved recipes matching the meal type
     */
    List<SavedRecipe> findByUserIdAndMealType(Long userId, SavedRecipe.MealType mealType);

    /**
     * Finds recently saved recipes for a specific user (ordered by creation date)
     * 
     * @param userId User's ID
     * @param limit Maximum number of recipes to return
     * @return List of recently saved recipes
     */
    @Query("SELECT r FROM SavedRecipe r WHERE r.user.id = :userId " +
           "ORDER BY r.createdAt DESC")
    List<SavedRecipe> findRecentRecipes(@Param("userId") Long userId);
}




