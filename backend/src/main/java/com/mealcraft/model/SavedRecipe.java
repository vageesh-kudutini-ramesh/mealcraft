package com.mealcraft.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SavedRecipe Entity
 * 
 * Represents a recipe that the user has saved from recipe suggestions.
 * Users can save recipes they like, customize them, and use them in meal planning.
 * 
 * Recipes are suggested by the system (via Spoonacular API) based on pantry ingredients.
 * Users do NOT create recipes from scratch - they save suggested recipes.
 * 
 * @author MealCraft Team
 */
@Entity
@Table(name = "saved_recipes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SavedRecipe {

    /**
     * Unique identifier for the saved recipe
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Recipe name/title
     * Required field
     */
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Recipe name is required")
    private String recipeName;

    /**
     * Meal type category (Breakfast, Lunch, Dinner, All)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Meal type is required")
    private MealType mealType;

    /**
     * Recipe image URL (from Spoonacular API or custom)
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * Estimated preparation time in minutes
     */
    @Column
    @Positive(message = "Prep time must be positive")
    private Integer prepTimeMinutes;

    /**
     * Estimated cooking time in minutes
     */
    @Column
    @Positive(message = "Cook time must be positive")
    private Integer cookTimeMinutes;

    /**
     * Number of servings
     */
    @Column
    @Positive(message = "Servings must be positive")
    private Integer servings;

    /**
     * Complete ingredients list stored as JSON
     * Format: [{"name": "Eggs", "quantity": 2, "unit": "pieces"}, ...]
     */
    @Column(columnDefinition = "TEXT")
    private String ingredientsJson;

    /**
     * Step-by-step cooking instructions
     * Can be customized by user after saving
     */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /**
     * User's custom notes about the recipe
     * Optional field for personalization
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Match percentage when recipe was suggested
     * Indicates how many ingredients user had available
     */
    @Column
    private Double matchPercentage;

    /**
     * External recipe ID from Spoonacular API (if applicable)
     * Used to fetch additional details if needed
     */
    @Column
    private Long externalRecipeId;

    /**
     * Foreign key reference to the user who saved this recipe
     * Many saved recipes belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Timestamp when recipe was saved
     * Automatically set on creation
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when recipe was last modified
     * Automatically updated on modification
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * One-to-many relationship with meal plans
     * A saved recipe can be used in multiple meal plans
     */
    @OneToMany(mappedBy = "savedRecipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPlan> mealPlans = new ArrayList<>();

    /**
     * Enum for meal type categories
     */
    public enum MealType {
        BREAKFAST("Breakfast"),
        LUNCH("Lunch"),
        DINNER("Dinner"),
        ALL("All");

        private final String displayName;

        MealType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

