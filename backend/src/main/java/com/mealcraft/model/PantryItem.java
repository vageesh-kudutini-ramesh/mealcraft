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
 * PantryItem Entity
 * 
 * Represents an item in the user's pantry inventory.
 * Tracks item name, quantity, unit, category, expiration date, and low-stock threshold.
 * 
 * Categories:
 * 1. FRUITS_VEGETABLES - Fruits & Vegetables
 * 2. DAIRY_PRODUCTS - Dairy Products
 * 3. PANTRY_STAPLES - Pantry Staples (rice, flour, oil, etc.)
 * 4. CONDIMENTS_SPICES - Condiments & Spices
 * 
 * @author MealCraft Team
 */
@Entity
@Table(name = "pantry_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PantryItem {

    /**
     * Unique identifier for the pantry item
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the pantry item (e.g., "Milk", "Tomatoes", "Rice")
     * Required field
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Item name is required")
    private String itemName;

    /**
     * Quantity of the item
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
     * Category of the pantry item
     * Uses enum for type safety
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Category is required")
    private PantryCategory category;

    /**
     * Expiration date of the item
     * Required field - used for expiration tracking and notifications
     */
    @Column(nullable = false)
    @NotNull(message = "Expiration date is required")
    private LocalDate expirationDate;

    /**
     * Custom threshold value for low-stock alerts
     * When current quantity falls below this threshold, low-stock alert is triggered
     */
    @Column(nullable = false)
    @NotNull(message = "Threshold is required")
    @Positive(message = "Threshold must be positive")
    private Double threshold;

    /**
     * Foreign key reference to the user who owns this pantry item
     * Many pantry items belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Timestamp when pantry item was created
     * Automatically set on creation
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when pantry item was last modified
     * Automatically updated on modification
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculates days until expiration
     * @return Number of days until expiration (negative if expired)
     */
    public long getDaysUntilExpiry() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    /**
     * Determines expiration status based on days until expiry.
     * EXPIRING_SOON = within 7 days (aligned with Pantry tab and notifications).
     * @return ExpirationStatus enum value
     */
    public ExpirationStatus getExpirationStatus() {
        long daysUntilExpiry = getDaysUntilExpiry();
        if (daysUntilExpiry < 0) {
            return ExpirationStatus.EXPIRED;
        } else if (daysUntilExpiry <= 7) {
            return ExpirationStatus.EXPIRING_SOON;
        } else {
            return ExpirationStatus.FRESH;
        }
    }

    /**
     * Checks if item is low in stock
     * @return true if current quantity is below threshold
     */
    public boolean isLowStock() {
        return quantity < threshold;
    }

    /**
     * Enum for pantry item categories
     */
    public enum PantryCategory {
        FRUITS_VEGETABLES("🥕 Fruits & Vegetables"),
        DAIRY_PRODUCTS("🥛 Dairy Products"),
        PANTRY_STAPLES("🍚 Pantry Staples"),
        CONDIMENTS_SPICES("🧂 Condiments & Spices");

        private final String displayName;

        PantryCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for expiration status
     */
    public enum ExpirationStatus {
        FRESH("🟢 Fresh"),
        EXPIRING_SOON("🟡 Expiring Soon"),
        EXPIRED("🔴 Expired");

        private final String displayName;

        ExpirationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

