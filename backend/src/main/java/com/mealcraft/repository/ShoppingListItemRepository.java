package com.mealcraft.repository;

import com.mealcraft.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<ShoppingListItem> findByUserId(Long userId);

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
    @Query("DELETE FROM ShoppingListItem s WHERE s.user.id = :userId AND s.isPurchased = true")
    void deletePurchasedItems(@Param("userId") Long userId);
}




