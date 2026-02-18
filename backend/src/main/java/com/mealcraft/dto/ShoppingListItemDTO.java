package com.mealcraft.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mealcraft.model.PantryItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for shopping list item operations
 * 
 * Used for creating, updating, and displaying shopping list items.
 * Supports both auto-generated (from meal plans) and manually added items.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListItemDTO {

    /**
     * Shopping list item ID (null for new items)
     */
    private Long id;

    /**
     * Name of the item to purchase
     * Required field
     */
    @NotBlank(message = "Item name is required")
    private String itemName;

    /**
     * Quantity to purchase
     * Must be positive
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    /**
     * Unit of measurement (e.g., "pieces", "grams", "ml", "kg", "cups", "tbsp")
     * Required field
     */
    @NotBlank(message = "Unit is required")
    private String unit;

    /**
     * Whether the item has been purchased
     * Default: false
     */
    private Boolean isPurchased = false;

    /**
     * Suggested expiration date (auto-calculated based on item type)
     * Optional - helps user set expiration when adding to pantry
     */
    private LocalDate suggestedExpirationDate;

    /**
     * Category of the item (for organization)
     * Optional
     */
    private PantryItem.PantryCategory category;

    /** Source: MANUAL or MEAL_PLAN_WEEK (for undo). */
    private String sourceType;

    /** When source is MEAL_PLAN_WEEK, week start date yyyy-MM-dd. */
    private String sourceWeekStart;

    /** Date when the item was added to the shopping list (from createdAt). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate addedAt;

    /** Date when the item was marked as purchased (null if not yet purchased). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate purchasedAt;
}




