package com.mealcraft.controller;

import com.mealcraft.dto.CookRecipeRequest;
import com.mealcraft.dto.RecipeDTO;
import com.mealcraft.dto.RecipeSuggestionRequest;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Recipe Controller
 * 
 * Handles recipe suggestion and saved recipe management endpoints.
 * Integrates with Spoonacular API for recipe suggestions.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Suggests recipes based on pantry ingredients
     * 
     * POST /api/recipes/suggest
     * 
     * @param authentication Spring Security authentication object
     * @param request Recipe suggestion request with meal type filter
     * @return List of suggested RecipeDTO
     */
    /**
     * Gets list of cuisines/areas from TheMealDB (e.g. Indian, American, Italian).
     * 
     * GET /api/recipes/areas
     */
    @GetMapping("/areas")
    public ResponseEntity<List<String>> getAreas(Authentication authentication) {
        getCurrentUser(authentication); // require auth
        List<String> areas = recipeService.getAreas();
        return ResponseEntity.ok(areas);
    }

    /** GET /api/recipes/discover?cuisine=&diet=&query=&dietaryRules= - Browse recipes. dietaryRules JSON (e.g. {"NO_GLUTEN":true}) from Meal Plan preferences. */
    @GetMapping("/discover")
    public ResponseEntity<List<RecipeDTO>> discoverRecipes(
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String diet,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String dietaryRules,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        try {
            Map<String, Object> rules = null;
            if (dietaryRules != null && !dietaryRules.trim().isEmpty()) {
                try {
                    rules = objectMapper.readValue(dietaryRules, new TypeReference<Map<String, Object>>() {});
                } catch (Exception ignored) { /* invalid JSON, ignore */ }
            }
            List<RecipeDTO> recipes = recipeService.discoverRecipes(query, cuisine, diet, rules, offset != null ? offset : 0);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            System.err.println("[RecipeController] discover error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/suggest")
    public ResponseEntity<List<RecipeDTO>> suggestRecipes(
            Authentication authentication,
            @RequestBody(required = false) RecipeSuggestionRequest request) {
        User user = getCurrentUser(authentication);
        
        if (request == null) {
            request = new RecipeSuggestionRequest();
        }
        
        List<RecipeDTO> suggestions = recipeService.suggestRecipes(user.getId(), request, request.getOffset() != null ? request.getOffset() : 0);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Gets all saved recipes for current user
     * 
     * GET /api/recipes/saved
     * 
     * @param authentication Spring Security authentication object
     * @return List of saved RecipeDTO
     */
    @GetMapping("/saved")
    public ResponseEntity<List<RecipeDTO>> getSavedRecipes(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<RecipeDTO> recipes = recipeService.getSavedRecipes(user.getId());
        return ResponseEntity.ok(recipes);
    }

    /**
     * Searches saved recipes by name
     * 
     * GET /api/recipes/saved/search?q={query}
     * 
     * @param authentication Spring Security authentication object
     * @param query Search query
     * @return List of matching RecipeDTO
     */
    @GetMapping("/saved/search")
    public ResponseEntity<List<RecipeDTO>> searchSavedRecipes(
            Authentication authentication,
            @RequestParam String q) {
        User user = getCurrentUser(authentication);
        List<RecipeDTO> recipes = recipeService.searchSavedRecipes(user.getId(), q);
        return ResponseEntity.ok(recipes);
    }

    /**
     * Gets a saved recipe by ID
     * 
     * GET /api/recipes/saved/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Recipe ID
     * @return RecipeDTO
     */
    @GetMapping("/saved/{id}")
    public ResponseEntity<RecipeDTO> getSavedRecipe(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        RecipeDTO recipe = recipeService.getSavedRecipe(user.getId(), id);
        return ResponseEntity.ok(recipe);
    }

    /**
     * Saves a recipe to user's collection
     * 
     * POST /api/recipes/saved
     * 
     * @param authentication Spring Security authentication object
     * @param recipeDTO Recipe to save
     * @return Saved RecipeDTO
     */
    @PostMapping("/saved")
    public ResponseEntity<RecipeDTO> saveRecipe(
            Authentication authentication,
            @Valid @RequestBody RecipeDTO recipeDTO) {
        User user = getCurrentUser(authentication);
        RecipeDTO saved = recipeService.saveRecipe(user.getId(), recipeDTO);
        return ResponseEntity.ok(saved);
    }

    /**
     * Updates a saved recipe
     * 
     * PUT /api/recipes/saved/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Recipe ID
     * @param recipeDTO Updated recipe data
     * @return Updated RecipeDTO
     */
    @PutMapping("/saved/{id}")
    public ResponseEntity<RecipeDTO> updateSavedRecipe(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RecipeDTO recipeDTO) {
        User user = getCurrentUser(authentication);
        RecipeDTO updated = recipeService.updateSavedRecipe(user.getId(), id, recipeDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a saved recipe
     * 
     * DELETE /api/recipes/saved/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Recipe ID
     * @return Success response
     */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<?> deleteSavedRecipe(Authentication authentication, @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        recipeService.deleteSavedRecipe(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets enhanced recipe details with pantry matching
     * 
     * GET /api/recipes/enhance/{recipeId}
     * 
     * @param authentication Spring Security authentication object
     * @param recipeId Recipe ID (external or saved)
     * @return Enhanced RecipeDTO with pantry matching
     */
    @GetMapping("/enhance/{recipeId}")
    public ResponseEntity<?> getEnhancedRecipeDetails(
            Authentication authentication,
            @PathVariable Long recipeId) {
        try {
            System.out.println("[RecipeController] getEnhancedRecipeDetails called for recipeId: " + recipeId);
            System.out.println("[RecipeController] Authentication object: " + (authentication != null ? "EXISTS" : "NULL"));
            if (authentication != null) {
                System.out.println("[RecipeController] Principal: " + authentication.getPrincipal().getClass().getName());
                System.out.println("[RecipeController] Authenticated: " + authentication.isAuthenticated());
            }
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("[RecipeController] ✗ Authentication failed - returning 401");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Authentication required",
                    "message", "Please log in to access this resource"
                ));
            }
            
            User user = getCurrentUser(authentication);
            System.out.println("[RecipeController] ✓ User authenticated: " + user.getEmail());
            RecipeDTO recipe = recipeService.getEnhancedRecipeDetails(user.getId(), recipeId);
            System.out.println("[RecipeController] ✓ Recipe details retrieved successfully");
            return ResponseEntity.ok(recipe);
        } catch (Exception e) {
            System.err.println("[RecipeController] ✗ Error in getEnhancedRecipeDetails: " + e.getMessage());
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Recipe not found with ID")) {
                return ResponseEntity.status(404).body(Map.of("error", msg));
            }
            if (msg.contains("temporarily unavailable") || msg.contains("external API")) {
                return ResponseEntity.status(502).body(Map.of("error", msg));
            }
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get recipe details: " + msg));
        }
    }

    /**
     * Marks pantry items as used when cooking a recipe
     * 
     * POST /api/recipes/cook
     * 
     * @param authentication Spring Security authentication object
     * @param request Cook recipe request with adjusted ingredients
     * @return Success response
     */
    @PostMapping("/cook")
    public ResponseEntity<?> cookRecipe(
            Authentication authentication,
            @RequestBody CookRecipeRequest request) {
        User user = getCurrentUser(authentication);
        recipeService.cookRecipe(user.getId(), request.getRecipeId(), request.getAdjustedIngredients());
        return ResponseEntity.ok().build();
    }

    /**
     * Helper method to get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication is null");
        }
        
        try {
            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
            return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (ClassCastException e) {
            throw new RuntimeException("Invalid authentication principal type: " + e.getMessage());
        }
    }
}




