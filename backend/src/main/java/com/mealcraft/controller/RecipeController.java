package com.mealcraft.controller;

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

import java.util.List;

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

    /**
     * Suggests recipes based on pantry ingredients
     * 
     * POST /api/recipes/suggest
     * 
     * @param authentication Spring Security authentication object
     * @param request Recipe suggestion request with meal type filter
     * @return List of suggested RecipeDTO
     */
    @PostMapping("/suggest")
    public ResponseEntity<List<RecipeDTO>> suggestRecipes(
            Authentication authentication,
            @RequestBody(required = false) RecipeSuggestionRequest request) {
        User user = getCurrentUser(authentication);
        
        if (request == null) {
            request = new RecipeSuggestionRequest();
        }
        
        List<RecipeDTO> suggestions = recipeService.suggestRecipes(user.getId(), request);
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
     * Helper method to get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}




