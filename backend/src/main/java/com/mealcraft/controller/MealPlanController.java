package com.mealcraft.controller;

import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.MealPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Meal Plan Controller
 * 
 * Handles weekly meal planning endpoints.
 * Supports creating meal plans from saved recipes or quick-add recipes.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/meal-plans")
@CrossOrigin(origins = "*")
public class MealPlanController {

    @Autowired
    private MealPlanService mealPlanService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets weekly meal plan
     * 
     * GET /api/meal-plans/week?startDate={startDate}&endDate={endDate}
     * 
     * @param authentication Spring Security authentication object
     * @param startDate Start date of the week
     * @param endDate End date of the week
     * @return List of MealPlanDTO
     */
    @GetMapping("/week")
    public ResponseEntity<List<MealPlanDTO>> getWeeklyMealPlan(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User user = getCurrentUser(authentication);
        List<MealPlanDTO> mealPlans = mealPlanService.getWeeklyMealPlan(user.getId(), startDate, endDate);
        return ResponseEntity.ok(mealPlans);
    }

    /**
     * Gets meal plan for a specific date
     * 
     * GET /api/meal-plans/date?date={date}
     * 
     * @param authentication Spring Security authentication object
     * @param date Date to get meal plan for
     * @return List of MealPlanDTO
     */
    @GetMapping("/date")
    public ResponseEntity<List<MealPlanDTO>> getMealPlanByDate(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = getCurrentUser(authentication);
        List<MealPlanDTO> mealPlans = mealPlanService.getMealPlanByDate(user.getId(), date);
        return ResponseEntity.ok(mealPlans);
    }

    /**
     * Creates a meal plan
     * 
     * POST /api/meal-plans
     * 
     * @param authentication Spring Security authentication object
     * @param mealPlanDTO Meal plan data
     * @return Created MealPlanDTO
     */
    @PostMapping
    public ResponseEntity<MealPlanDTO> createMealPlan(
            Authentication authentication,
            @Valid @RequestBody MealPlanDTO mealPlanDTO) {
        User user = getCurrentUser(authentication);
        MealPlanDTO created = mealPlanService.createMealPlanFromSavedRecipe(user.getId(), mealPlanDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Updates a meal plan
     * 
     * PUT /api/meal-plans/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Meal plan ID
     * @param mealPlanDTO Updated meal plan data
     * @return Updated MealPlanDTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<MealPlanDTO> updateMealPlan(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MealPlanDTO mealPlanDTO) {
        User user = getCurrentUser(authentication);
        MealPlanDTO updated = mealPlanService.updateMealPlan(user.getId(), id, mealPlanDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a meal plan
     * 
     * DELETE /api/meal-plans/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Meal plan ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMealPlan(Authentication authentication, @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        mealPlanService.deleteMealPlan(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Clears meal plan for a specific date
     * 
     * DELETE /api/meal-plans/date?date={date}
     * 
     * @param authentication Spring Security authentication object
     * @param date Date to clear
     * @return Success response
     */
    @DeleteMapping("/date")
    public ResponseEntity<?> clearMealPlanForDate(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = getCurrentUser(authentication);
        mealPlanService.clearMealPlanForDate(user.getId(), date);
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




