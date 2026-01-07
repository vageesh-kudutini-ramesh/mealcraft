package com.mealcraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.model.MealPlan;
import com.mealcraft.model.SavedRecipe;
import com.mealcraft.model.User;
import com.mealcraft.repository.MealPlanRepository;
import com.mealcraft.repository.SavedRecipeRepository;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Meal Plan Service
 * 
 * Handles weekly meal planning operations.
 * Supports creating meal plans from saved recipes or quick-add recipes.
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class MealPlanService {

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Gets weekly meal plan for a user
     * 
     * @param userId User's ID
     * @param startDate Start date of the week
     * @param endDate End date of the week
     * @return List of MealPlanDTO
     */
    public List<MealPlanDTO> getWeeklyMealPlan(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MealPlan> mealPlans = mealPlanRepository.findWeeklyMealPlans(userId, startDate, endDate);
        return mealPlans.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets meal plan for a specific date
     * 
     * @param userId User's ID
     * @param date Date to get meal plan for
     * @return List of MealPlanDTO for the date
     */
    public List<MealPlanDTO> getMealPlanByDate(Long userId, LocalDate date) {
        List<MealPlan> mealPlans = mealPlanRepository.findByUserIdAndDate(userId, date);
        return mealPlans.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Creates meal plan from saved recipe
     * 
     * @param userId User's ID
     * @param mealPlanDTO Meal plan data
     * @return Created MealPlanDTO
     */
    public MealPlanDTO createMealPlanFromSavedRecipe(Long userId, MealPlanDTO mealPlanDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        SavedRecipe savedRecipe = null;
        if (mealPlanDTO.getSavedRecipeId() != null) {
            savedRecipe = savedRecipeRepository.findById(mealPlanDTO.getSavedRecipeId())
                .orElse(null);
        }

        MealPlan mealPlan = new MealPlan();
        mealPlan.setDate(mealPlanDTO.getDate());
        mealPlan.setMealType(mealPlanDTO.getMealType());
        mealPlan.setRecipeName(mealPlanDTO.getRecipeName());
        mealPlan.setImageUrl(mealPlanDTO.getImageUrl());
        mealPlan.setPrepTimeMinutes(mealPlanDTO.getPrepTimeMinutes());
        mealPlan.setCookTimeMinutes(mealPlanDTO.getCookTimeMinutes());
        mealPlan.setServings(mealPlanDTO.getServings());
        mealPlan.setInstructions(mealPlanDTO.getInstructions());
        mealPlan.setSavedRecipe(savedRecipe);
        mealPlan.setUser(user);

        // Convert ingredients to JSON (snapshot)
        try {
            String ingredientsJson = objectMapper.writeValueAsString(mealPlanDTO.getIngredients());
            mealPlan.setIngredientsJson(ingredientsJson);
        } catch (Exception e) {
            mealPlan.setIngredientsJson("[]");
        }

        mealPlan = mealPlanRepository.save(mealPlan);
        return mapToDTO(mealPlan);
    }

    /**
     * Updates a meal plan
     * 
     * @param userId User's ID
     * @param mealPlanId Meal plan ID
     * @param mealPlanDTO Updated meal plan data
     * @return Updated MealPlanDTO
     */
    public MealPlanDTO updateMealPlan(Long userId, Long mealPlanId, MealPlanDTO mealPlanDTO) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
            .orElseThrow(() -> new RuntimeException("Meal plan not found"));

        if (!mealPlan.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Meal plan does not belong to user");
        }

        // Update fields
        mealPlan.setDate(mealPlanDTO.getDate());
        mealPlan.setMealType(mealPlanDTO.getMealType());
        mealPlan.setRecipeName(mealPlanDTO.getRecipeName());
        mealPlan.setImageUrl(mealPlanDTO.getImageUrl());
        mealPlan.setPrepTimeMinutes(mealPlanDTO.getPrepTimeMinutes());
        mealPlan.setCookTimeMinutes(mealPlanDTO.getCookTimeMinutes());
        mealPlan.setServings(mealPlanDTO.getServings());
        mealPlan.setInstructions(mealPlanDTO.getInstructions());

        // Update ingredients JSON
        try {
            String ingredientsJson = objectMapper.writeValueAsString(mealPlanDTO.getIngredients());
            mealPlan.setIngredientsJson(ingredientsJson);
        } catch (Exception e) {
            // Keep existing ingredients if conversion fails
        }

        mealPlan = mealPlanRepository.save(mealPlan);
        return mapToDTO(mealPlan);
    }

    /**
     * Deletes a meal plan
     * 
     * @param userId User's ID
     * @param mealPlanId Meal plan ID
     */
    public void deleteMealPlan(Long userId, Long mealPlanId) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
            .orElseThrow(() -> new RuntimeException("Meal plan not found"));

        if (!mealPlan.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Meal plan does not belong to user");
        }

        mealPlanRepository.delete(mealPlan);
    }

    /**
     * Deletes all meal plans for a specific date
     * 
     * @param userId User's ID
     * @param date Date to clear
     */
    public void clearMealPlanForDate(Long userId, LocalDate date) {
        mealPlanRepository.deleteByUserIdAndDate(userId, date);
    }

    /**
     * Maps MealPlan entity to MealPlanDTO
     * 
     * @param mealPlan MealPlan entity
     * @return MealPlanDTO
     */
    private MealPlanDTO mapToDTO(MealPlan mealPlan) {
        MealPlanDTO dto = new MealPlanDTO();
        dto.setId(mealPlan.getId());
        dto.setDate(mealPlan.getDate());
        dto.setMealType(mealPlan.getMealType());
        dto.setRecipeName(mealPlan.getRecipeName());
        dto.setImageUrl(mealPlan.getImageUrl());
        dto.setPrepTimeMinutes(mealPlan.getPrepTimeMinutes());
        dto.setCookTimeMinutes(mealPlan.getCookTimeMinutes());
        dto.setServings(mealPlan.getServings());
        dto.setInstructions(mealPlan.getInstructions());
        dto.setSavedRecipeId(mealPlan.getSavedRecipe() != null ? mealPlan.getSavedRecipe().getId() : null);

        // Parse ingredients JSON
        try {
            if (mealPlan.getIngredientsJson() != null) {
                JsonNode ingredientsJson = objectMapper.readTree(mealPlan.getIngredientsJson());
                List<Map<String, Object>> ingredients = new ArrayList<>();
                for (JsonNode ing : ingredientsJson) {
                    Map<String, Object> ingredient = new HashMap<>();
                    ingredient.put("name", ing.path("name").asText());
                    ingredient.put("quantity", ing.path("quantity").asDouble());
                    ingredient.put("unit", ing.path("unit").asText());
                    ingredients.add(ingredient);
                }
                dto.setIngredients(ingredients);
            }
        } catch (Exception e) {
            dto.setIngredients(new ArrayList<>());
        }

        return dto;
    }
}




