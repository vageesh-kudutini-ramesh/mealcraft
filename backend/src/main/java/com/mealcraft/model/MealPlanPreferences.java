package com.mealcraft.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User preferences for meal planning: week patterns (e.g. Meatless Monday)
 * and dietary rules (no gluten, min vegetarian dinners, etc.).
 */
@Entity
@Table(name = "meal_plan_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MealPlanPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * JSON array of enabled patterns. Each: { "key": "MEATLESS_MONDAY", "label": "Meatless Monday", "dayOfWeek": 1, "mealType": "DINNER", "dietFilter": "VEGETARIAN" }
     */
    @Column(columnDefinition = "TEXT")
    private String patternsJson;

    /**
     * JSON: { "noGluten": false, "minVegetarianDinnersPerWeek": 0, "maxCaloriesPerDinner": null }
     */
    @Column(columnDefinition = "TEXT")
    private String dietaryRulesJson;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
