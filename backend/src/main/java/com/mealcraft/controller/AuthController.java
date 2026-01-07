package com.mealcraft.controller;

import com.mealcraft.dto.AuthRequest;
import com.mealcraft.dto.AuthResponse;
import com.mealcraft.dto.RegisterRequest;
import com.mealcraft.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * 
 * Handles user registration and login endpoints.
 * Public endpoints - no authentication required.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Registers a new user
     * 
     * POST /api/auth/register
     * 
     * @param registerRequest Registration request containing user details
     * @return AuthResponse with JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse response = authService.register(registerRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + e.getMessage());
        }
    }

    /**
     * Authenticates user and returns JWT token
     * 
     * POST /api/auth/login
     * 
     * @param authRequest Authentication request containing email and password
     * @return AuthResponse with JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            AuthResponse response = authService.login(authRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Error: Invalid email or password");
        }
    }
}




