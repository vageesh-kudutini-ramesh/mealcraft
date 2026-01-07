package com.mealcraft.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication requests (Login)
 * 
 * Used for user login requests.
 * Contains email and password for authentication.
 * 
 * @author MealCraft Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /**
     * User's email address
     * Required field, must be valid email format
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's password
     * Required field
     */
    @NotBlank(message = "Password is required")
    private String password;
}




