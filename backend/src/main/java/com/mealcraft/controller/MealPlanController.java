package com.mealcraft.controller;

import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.dto.MealPlanPreferencesDTO;
import com.mealcraft.dto.WeekExportDTO;
import com.mealcraft.model.MealPlan;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.MealPlanPreferencesService;
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
import java.util.Map;

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
    private MealPlanPreferencesService preferencesService;

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

    /** GET /api/meal-plans/preferences */
    @GetMapping("/preferences")
    public ResponseEntity<MealPlanPreferencesDTO> getPreferences(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(preferencesService.getPreferences(user.getId()));
    }

    /** PUT /api/meal-plans/preferences */
    @PutMapping("/preferences")
    public ResponseEntity<MealPlanPreferencesDTO> savePreferences(
            Authentication authentication,
            @RequestBody MealPlanPreferencesDTO dto) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(preferencesService.savePreferences(user.getId(), dto));
    }

    /** POST /api/meal-plans/apply-patterns?weekStart={date} */
    @PostMapping("/apply-patterns")
    public ResponseEntity<List<MealPlanDTO>> applyPatterns(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        User user = getCurrentUser(authentication);
        List<MealPlanDTO> created = mealPlanService.applyPatternsToWeek(user.getId(), weekStart);
        return ResponseEntity.ok(created);
    }

    /** POST /api/meal-plans/revert-patterns?weekStart={date} */
    @PostMapping("/revert-patterns")
    public ResponseEntity<Map<String, Integer>> revertPatterns(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        User user = getCurrentUser(authentication);
        int count = mealPlanService.revertPatternsForWeek(user.getId(), weekStart);
        return ResponseEntity.ok(Map.of("removedCount", count));
    }

    /** GET /api/meal-plans/export?startDate=&endDate=&includeShopping= */
    @GetMapping("/export")
    public ResponseEntity<WeekExportDTO> exportWeek(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean includeShopping) {
        User user = getCurrentUser(authentication);
        WeekExportDTO export = mealPlanService.getWeekExport(user.getId(), startDate, endDate, includeShopping);
        return ResponseEntity.ok(export);
    }

    /** GET /api/meal-plans/{id}/leftover-suggestions */
    @GetMapping("/{id}/leftover-suggestions")
    public ResponseEntity<List<Map<String, Object>>> leftoverSuggestions(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(mealPlanService.suggestLeftoverSlots(user.getId(), id));
    }

    /** POST /api/meal-plans/leftover */
    @PostMapping("/leftover")
    public ResponseEntity<MealPlanDTO> addLeftover(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        User user = getCurrentUser(authentication);
        Long sourceId = Long.valueOf(body.get("sourceMealPlanId").toString());
        LocalDate date = LocalDate.parse(body.get("date").toString());
        MealPlan.MealType mealType = MealPlan.MealType.valueOf(body.get("mealType").toString());
        MealPlanDTO created = mealPlanService.addLeftoverToSlot(user.getId(), sourceId, date, mealType);
        return ResponseEntity.ok(created);
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




