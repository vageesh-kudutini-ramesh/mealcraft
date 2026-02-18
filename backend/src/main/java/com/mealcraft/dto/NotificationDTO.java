package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for in-app notifications (bell icon).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    /** Unique client-facing id (e.g. type + ref) */
    private String id;

    /** Type for grouping and dismiss logic */
    private String type;

    /** Short title */
    private String title;

    /** Main message */
    private String message;

    /** Optional link to navigate (e.g. /pantry, /meal-plan) */
    private String actionUrl;

    /** Reference for dismiss (e.g. pantry item id, "streak") */
    private String referenceId;

    /** Severity: info, warning, success, tip */
    private String severity;

    /** Icon name for frontend (e.g. clock, shopping-cart, chef-hat) */
    private String icon;

    /** For expiring items: extra context (e.g. "Yogurt - 2 days") */
    private String subtitle;

    /** Recipe matches for expiring+recipe notification */
    private List<RecipeDTO> recipeMatches;
}
