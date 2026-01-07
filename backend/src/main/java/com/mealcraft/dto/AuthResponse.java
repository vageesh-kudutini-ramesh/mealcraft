package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses
 * 
 * Returned after successful login or registration.
 * Contains JWT token and user information.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT token for authenticated requests
     * Client should include this in Authorization header for protected endpoints
     */
    private String token;

    /**
     * Token type (usually "Bearer")
     */
    private String type = "Bearer";

    /**
     * User's ID
     */
    private Long userId;

    /**
     * User's email address
     */
    private String email;

    /**
     * User's full name
     */
    private String fullName;

    /**
     * User's initials (for profile icon)
     */
    private String initials;
}




