package com.mealcraft.repository;

import com.mealcraft.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MealPlan entity
 * 
 * Provides CRUD operations and custom query methods for meal planning.
 * Includes methods for weekly meal plan retrieval.
 * 
 * @author MealCraft Team
 */
@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    /**
     * Finds all meal plans for a specific user
     * 
     * @param userId User's ID
     * @return List of meal plans belonging to the user
     */
    List<MealPlan> findByUserId(Long userId);

    /**
     * Finds meal plans for a specific date and user
     * 
     * @param userId User's ID
     * @param date Date to search for
     * @return List of meal plans for the specified date
     */
    List<MealPlan> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Finds meal plan for a specific date, meal type, and user
     * 
     * @param userId User's ID
     * @param date Date to search for
     * @param mealType Meal type (Breakfast, Lunch, Dinner)
     * @return Optional containing MealPlan if found, empty otherwise
     */
    Optional<MealPlan> findByUserIdAndDateAndMealType(Long userId, LocalDate date, MealPlan.MealType mealType);

    /**
     * Finds meal plans for a date range (weekly meal plan)
     * 
     * @param userId User's ID
     * @param startDate Start date of the week
     * @param endDate End date of the week
     * @return List of meal plans in the specified date range
     */
    @Query("SELECT m FROM MealPlan m WHERE m.user.id = :userId " +
           "AND m.date >= :startDate AND m.date <= :endDate " +
           "ORDER BY m.date ASC, m.mealType ASC")
    List<MealPlan> findWeeklyMealPlans(@Param("userId") Long userId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Deletes all meal plans for a specific date and user
     * Used for clearing a day's meal plan
     * 
     * @param userId User's ID
     * @param date Date to clear
     */
    void deleteByUserIdAndDate(Long userId, LocalDate date);
}




