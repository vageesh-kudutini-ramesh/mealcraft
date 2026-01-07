package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user profile information
 * 
 * Used for displaying and updating user profile.
 * Contains editable profile fields (firstName, lastName, age).
 * Email is display-only and cannot be changed.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    /**
     * User's ID
     */
    private Long id;

    /**
     * User's email address (display only, cannot be changed)
     */
    private String email;

    /**
     * User's first name (editable)
     */
    private String firstName;

    /**
     * User's last name (editable)
     */
    private String lastName;

    /**
     * User's age (optional, editable)
     */
    private Integer age;

    /**
     * User's full name (computed)
     */
    private String fullName;

    /**
     * User's initials for profile icon (computed)
     */
    private String initials;
}




