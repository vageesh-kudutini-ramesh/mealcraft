package com.mealcraft.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks which meal plan slots (date|mealType) have been synced to the shopping list.
 * Used for incremental add: only add ingredients from NEW slots, not already-added ones.
 */
@Entity
@Table(name = "meal_plan_shopping_sync", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "week_start", "slot_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanShoppingSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start", nullable = false, length = 10)
    private String weekStart;

    /** Slot key = date|mealType e.g. "2025-02-01|BREAKFAST" */
    @Column(name = "slot_key", nullable = false, length = 30)
    private String slotKey;
}
