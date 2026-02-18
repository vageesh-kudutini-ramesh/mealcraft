package com.mealcraft.service;

import com.mealcraft.dto.AuthRequest;
import com.mealcraft.dto.AuthResponse;
import com.mealcraft.dto.RegisterRequest;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * 
 * Handles user registration and login operations.
 * Manages JWT token generation and password encryption.
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Registers a new user
     * 
     * @param registerRequest Registration request containing user details
     * @return AuthResponse with JWT token and user information
     * @throws RuntimeException if email already exists
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists: " + registerRequest.getEmail());
        }

        // Create new user
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Encrypt password
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setAge(registerRequest.getAge());

        // Save user to database
        user = userRepository.save(user);

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        // Build and return response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setInitials(user.getInitials());

        return response;
    }

    /**
     * Authenticates user and generates JWT token
     * 
     * @param authRequest Authentication request containing email and password
     * @return AuthResponse with JWT token and user information
     * @throws RuntimeException if authentication fails
     */
    public AuthResponse login(AuthRequest authRequest) {
        // Authenticate user credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authRequest.getEmail(),
                authRequest.getPassword()
            )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        
        // Find user entity for additional information
        User user = userRepository.findByEmail(authRequest.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(userDetails);

        // Build and return response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setInitials(user.getInitials());
        return response;
    }
}




