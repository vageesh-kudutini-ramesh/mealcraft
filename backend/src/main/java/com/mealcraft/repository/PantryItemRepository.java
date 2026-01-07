package com.mealcraft.repository;

import com.mealcraft.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for PantryItem entity
 * 
 * Provides CRUD operations and custom query methods for pantry inventory management.
 * Includes methods for expiration tracking and low-stock alerts.
 * 
 * @author MealCraft Team
 */
@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    /**
     * Finds all pantry items for a specific user
     * 
     * @param userId User's ID
     * @return List of pantry items belonging to the user
     */
    List<PantryItem> findByUserId(Long userId);

    /**
     * Finds pantry items expiring within a specified number of days
     * Used for expiration notifications (1-5 days before expiry)
     * 
     * @param userId User's ID
     * @param startDate Start date for expiration range (today)
     * @param endDate End date for expiration range (today + days)
     * @return List of pantry items expiring in the specified range
     */
    @Query("SELECT p FROM PantryItem p WHERE p.user.id = :userId " +
           "AND p.expirationDate >= :startDate AND p.expirationDate <= :endDate " +
           "ORDER BY p.expirationDate ASC")
    List<PantryItem> findExpiringItems(@Param("userId") Long userId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Finds expired pantry items (expiration date is in the past)
     * 
     * @param userId User's ID
     * @param today Current date
     * @return List of expired pantry items
     */
    @Query("SELECT p FROM PantryItem p WHERE p.user.id = :userId " +
           "AND p.expirationDate < :today ORDER BY p.expirationDate ASC")
    List<PantryItem> findExpiredItems(@Param("userId") Long userId,
                                      @Param("today") LocalDate today);

    /**
     * Finds low-stock pantry items (quantity below threshold)
     * 
     * @param userId User's ID
     * @return List of low-stock items
     */
    @Query("SELECT p FROM PantryItem p WHERE p.user.id = :userId " +
           "AND p.quantity < p.threshold ORDER BY p.itemName ASC")
    List<PantryItem> findLowStockItems(@Param("userId") Long userId);

    /**
     * Finds pantry items by category for a specific user
     * 
     * @param userId User's ID
     * @param category Pantry category
     * @return List of pantry items in the specified category
     */
    List<PantryItem> findByUserIdAndCategory(Long userId, PantryItem.PantryCategory category);

    /**
     * Deletes all expired items for a specific user
     * Used for bulk cleanup of expired items
     * 
     * @param userId User's ID
     * @param today Current date
     */
    @Query("DELETE FROM PantryItem p WHERE p.user.id = :userId AND p.expirationDate < :today")
    void deleteExpiredItems(@Param("userId") Long userId, @Param("today") LocalDate today);
}




