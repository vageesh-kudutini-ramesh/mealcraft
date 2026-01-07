package com.mealcraft.controller;

import com.mealcraft.dto.UserProfileDTO;
import com.mealcraft.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 * 
 * Handles user profile management endpoints.
 * Protected endpoints - requires JWT authentication.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Gets current user's profile
     * 
     * GET /api/users/profile
     * 
     * @param authentication Spring Security authentication object
     * @return UserProfileDTO with user profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        UserProfileDTO profile = userService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates current user's profile
     * 
     * PUT /api/users/profile
     * 
     * @param authentication Spring Security authentication object
     * @param profileDTO Updated profile information
     * @return Updated UserProfileDTO
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileDTO profileDTO) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        UserProfileDTO updatedProfile = userService.updateUserProfile(email, profileDTO);
        return ResponseEntity.ok(updatedProfile);
    }
}




