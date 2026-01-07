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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ShoppingListItem Entity
 * 
 * Represents an item in the user's shopping list.
 * Shopping lists can be:
 * 1. Auto-generated from meal plans (calculates needed ingredients)
 * 2. Manually added by the user
 * 
 * When items are purchased, they can be moved from shopping list to pantry.
 * 
 * @author MealCraft Team
 */
@Entity
@Table(name = "shopping_list_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShoppingListItem {

    /**
     * Unique identifier for the shopping list item
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the item to purchase
     * Required field
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Item name is required")
    private String itemName;

    /**
     * Quantity to purchase
     * Must be positive
     */
    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    /**
     * Unit of measurement (e.g., "pieces", "grams", "ml", "kg", "cups", "tbsp")
     * Required field
     */
    @Column(nullable = false, length = 20)
    @NotBlank(message = "Unit is required")
    private String unit;

    /**
     * Whether the item has been purchased
     * Default: false
     */
    @Column(nullable = false)
    private Boolean isPurchased = false;

    /**
     * Suggested expiration date (auto-calculated based on item type)
     * Optional - helps user set expiration when adding to pantry
     */
    @Column
    private LocalDate suggestedExpirationDate;

    /**
     * Category of the item (for organization)
     * Optional - can be used for grouping in shopping list
     */
    @Enumerated(EnumType.STRING)
    @Column
    private PantryItem.PantryCategory category;

    /**
     * Foreign key reference to the user who owns this shopping list item
     * Many shopping list items belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Timestamp when shopping list item was created
     * Automatically set on creation
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when shopping list item was last modified
     * Automatically updated on modification
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Marks the item as purchased
     */
    public void markAsPurchased() {
        this.isPurchased = true;
    }

    /**
     * Marks the item as not purchased
     */
    public void markAsNotPurchased() {
        this.isPurchased = false;
    }
}

