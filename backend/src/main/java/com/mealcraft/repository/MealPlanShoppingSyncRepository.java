package com.mealcraft.repository;

import com.mealcraft.model.MealPlanShoppingSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface MealPlanShoppingSyncRepository extends JpaRepository<MealPlanShoppingSync, Long> {

    @Query("SELECT s.slotKey FROM MealPlanShoppingSync s WHERE s.userId = :userId AND s.weekStart = :weekStart")
    Set<String> findSlotKeysByUserIdAndWeek(@Param("userId") Long userId, @Param("weekStart") String weekStart);

    @Modifying
    @Query("DELETE FROM MealPlanShoppingSync s WHERE s.userId = :userId AND s.weekStart = :weekStart")
    void deleteByUserIdAndWeek(@Param("userId") Long userId, @Param("weekStart") String weekStart);
}
