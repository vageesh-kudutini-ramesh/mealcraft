package com.mealcraft.repository;

import com.mealcraft.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ShoppingListItem entity
 * 
 * Provides CRUD operations and custom query methods for shopping list management.
 * Includes methods for filtering purchased/unpurchased items.
 * 
 * @author MealCraft Team
 */
@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    /**
     * Finds all shopping list items for a specific user
     * 
     * @param userId User's ID
     * @return List of shopping list items belonging to the user
     */
    @Query("SELECT s FROM ShoppingListItem s WHERE s.user.id = :userId ORDER BY s.createdAt ASC")
    List<ShoppingListItem> findByUserId(@Param("userId") Long userId);

    /**
     * Finds unpurchased shopping list items for a specific user
     * 
     * @param userId User's ID
     * @return List of unpurchased items
     */
    @Query("SELECT s FROM ShoppingListItem s WHERE s.user.id = :userId " +
           "AND s.isPurchased = false ORDER BY s.createdAt ASC")
    List<ShoppingListItem> findUnpurchasedItems(@Param("userId") Long userId);

    /**
     * Finds purchased shopping list items for a specific user
     * 
     * @param userId User's ID
     * @return List of purchased items
     */
    @Query("SELECT s FROM ShoppingListItem s WHERE s.user.id = :userId " +
           "AND s.isPurchased = true ORDER BY s.createdAt DESC")
    List<ShoppingListItem> findPurchasedItems(@Param("userId") Long userId);

    /**
     * Deletes all purchased items for a specific user
     * Used for clearing completed shopping list items
     * 
     * @param userId User's ID
     */
    @Modifying
    @Query("DELETE FROM ShoppingListItem s WHERE s.user.id = :userId AND s.isPurchased = true")
    void deletePurchasedItems(@Param("userId") Long userId);

    /**
     * Deletes all unpurchased (to-buy) items for a specific user
     * 
     * @param userId User's ID
     */
    @Modifying
    @Query("DELETE FROM ShoppingListItem s WHERE s.user.id = :userId AND s.isPurchased = false")
    void deleteUnpurchasedItems(@Param("userId") Long userId);

    /**
     * Finds items added from meal plan week (for undo).
     */
    @Query("SELECT s FROM ShoppingListItem s WHERE s.user.id = :userId " +
           "AND s.sourceType = 'MEAL_PLAN_WEEK' AND s.sourceWeekStart = :weekStart")
    List<ShoppingListItem> findByUserIdAndSourceWeek(@Param("userId") Long userId, @Param("weekStart") String weekStart);

    /**
     * Deletes items that were added from a specific week's meal plan (undo).
     */
    @Modifying
    @Query("DELETE FROM ShoppingListItem s WHERE s.user.id = :userId " +
           "AND s.sourceType = 'MEAL_PLAN_WEEK' AND s.sourceWeekStart = :weekStart")
    void deleteByUserIdAndSourceWeek(@Param("userId") Long userId, @Param("weekStart") String weekStart);

    /**
     * Counts items from a specific week (for "already added" check).
     */
    @Query("SELECT COUNT(s) FROM ShoppingListItem s WHERE s.user.id = :userId " +
           "AND s.sourceType = 'MEAL_PLAN_WEEK' AND s.sourceWeekStart = :weekStart")
    long countByUserIdAndSourceWeek(@Param("userId") Long userId, @Param("weekStart") String weekStart);
}




