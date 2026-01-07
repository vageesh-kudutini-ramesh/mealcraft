package com.mealcraft.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MealPlan Entity
 * 
 * Represents a meal planned for a specific date and meal type (Breakfast, Lunch, Dinner).
 * Users can plan meals for the week using a calendar interface with drag-and-drop functionality.
 * 
 * Meal plans can be created from:
 * 1. Saved recipes (dragged from saved recipes collection)
 * 2. Quick-add recipes (created directly in meal plan without saving)
 * 
 * Even if the original saved recipe is deleted, the meal plan retains a snapshot of the recipe data.
 * 
 * @author MealCraft Team
 */
@Entity
@Table(name = "meal_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MealPlan {

    /**
     * Unique identifier for the meal plan entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Date for which the meal is planned
     * Required field
     */
    @Column(nullable = false)
    @NotNull(message = "Date is required")
    private LocalDate date;

    /**
     * Meal type (Breakfast, Lunch, Dinner)
     * Required field
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Meal type is required")
    private MealType mealType;

    /**
     * Recipe name (snapshot - retained even if saved recipe is deleted)
     * Required field
     */
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Recipe name is required")
    private String recipeName;

    /**
     * Complete ingredients list stored as JSON (snapshot)
     * Format: [{"name": "Eggs", "quantity": 2, "unit": "pieces"}, ...]
     * This snapshot ensures meal plan data persists even if saved recipe is deleted
     */
    @Column(columnDefinition = "TEXT")
    private String ingredientsJson;

    /**
     * Step-by-step cooking instructions (snapshot)
     * Retained from saved recipe or entered directly for quick-add recipes
     */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /**
     * Recipe image URL (snapshot)
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * Estimated preparation time in minutes
     */
    @Column
    private Integer prepTimeMinutes;

    /**
     * Estimated cooking time in minutes
     */
    @Column
    private Integer cookTimeMinutes;

    /**
     * Number of servings
     */
    @Column
    private Integer servings;

    /**
     * Foreign key reference to the saved recipe (if meal plan was created from saved recipe)
     * Optional - can be null for quick-add recipes
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_recipe_id")
    private SavedRecipe savedRecipe;

    /**
     * Foreign key reference to the user who created this meal plan
     * Many meal plans belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Timestamp when meal plan was created
     * Automatically set on creation
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when meal plan was last modified
     * Automatically updated on modification
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum for meal type (Breakfast, Lunch, Dinner)
     */
    public enum MealType {
        BREAKFAST("Breakfast"),
        LUNCH("Lunch"),
        DINNER("Dinner");

        private final String displayName;

        MealType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

