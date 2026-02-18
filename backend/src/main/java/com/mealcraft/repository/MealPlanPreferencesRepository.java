package com.mealcraft.repository;

import com.mealcraft.model.MealPlanPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealPlanPreferencesRepository extends JpaRepository<MealPlanPreferences, Long> {

    Optional<MealPlanPreferences> findByUser_Id(Long userId);
}
