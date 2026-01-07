package com.mealcraft.dto;

import com.mealcraft.model.PantryItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for pantry item operations
 * 
 * Used for creating, updating, and displaying pantry items.
 * Contains all pantry item information including expiration tracking.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PantryItemDTO {

    /**
     * Pantry item ID (null for new items)
     */
    private Long id;

    /**
     * Item name (e.g., "Milk", "Tomatoes", "Rice")
     * Required field
     */
    @NotBlank(message = "Item name is required")
    private String itemName;

    /**
     * Quantity of the item
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
     * Category of the pantry item
     * Required field
     */
    @NotNull(message = "Category is required")
    private PantryItem.PantryCategory category;

    /**
     * Expiration date (YYYY-MM-DD format)
     * Required field - used for expiration tracking and notifications
     */
    @NotNull(message = "Expiration date is required")
    private LocalDate expirationDate;

    /**
     * Custom threshold value for low-stock alerts
     * When current quantity falls below this threshold, low-stock alert is triggered
     * Must be positive
     */
    @NotNull(message = "Threshold is required")
    @Positive(message = "Threshold must be positive")
    private Double threshold;

    /**
     * Days until expiration (computed, for display)
     * Negative if expired
     */
    private Long daysUntilExpiry;

    /**
     * Expiration status (FRESH, EXPIRING_SOON, EXPIRED)
     * Computed based on days until expiry
     */
    private PantryItem.ExpirationStatus expirationStatus;

    /**
     * Whether item is low in stock (computed)
     * true if quantity < threshold
     */
    private Boolean isLowStock;
}




