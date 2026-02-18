package com.mealcraft.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.dto.RecipeDTO;
import com.mealcraft.dto.RecipeSuggestionRequest;
import com.mealcraft.dto.WeekExportDTO;
import com.mealcraft.model.MealPlan;
import com.mealcraft.model.MealPlanPreferences;
import com.mealcraft.model.SavedRecipe;
import com.mealcraft.model.User;
import com.mealcraft.repository.MealPlanPreferencesRepository;
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
    private MealPlanPreferencesRepository preferencesRepository;

    @Autowired
    private RecipeService recipeService;

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
        mealPlan.setIsBatch(mealPlanDTO.getIsBatch() != null ? mealPlanDTO.getIsBatch() : false);
        mealPlan.setSourcePatternKey(mealPlanDTO.getSourcePatternKey());

        // Convert ingredients to JSON (normalize to name/quantity/unit for consistency)
        try {
            List<Map<String, Object>> normalized = normalizeIngredients(mealPlanDTO.getIngredients());
            mealPlan.setIngredientsJson(objectMapper.writeValueAsString(normalized));
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

        // Update ingredients JSON (normalize to name/quantity/unit)
        try {
            List<Map<String, Object>> normalized = normalizeIngredients(mealPlanDTO.getIngredients());
            mealPlan.setIngredientsJson(objectMapper.writeValueAsString(normalized));
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

        List<MealPlan> leftovers = mealPlanRepository.findByLeftoverOfMealPlan_Id(mealPlanId);
        if (!leftovers.isEmpty()) {
            throw new RuntimeException("BATCH_DELETE_LEFTOVERS_FIRST: Remove leftover entries from other days first, then delete this batch meal.");
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
     * Creates a meal plan entry from recipe details (e.g. from pattern suggestion). No saved recipe.
     */
    public MealPlanDTO createMealPlanFromRecipeDetails(Long userId, LocalDate date, MealPlan.MealType mealType,
                                                       String recipeName, List<Map<String, Object>> ingredients,
                                                       String instructions, String imageUrl,
                                                       Integer prepMinutes, Integer cookMinutes, Integer servings,
                                                       String sourcePatternKey) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        MealPlan mealPlan = new MealPlan();
        mealPlan.setDate(date);
        mealPlan.setMealType(mealType);
        mealPlan.setRecipeName(recipeName != null ? recipeName : "Recipe");
        mealPlan.setImageUrl(imageUrl);
        mealPlan.setPrepTimeMinutes(prepMinutes);
        mealPlan.setCookTimeMinutes(cookMinutes);
        mealPlan.setServings(servings != null ? servings : 4);
        mealPlan.setInstructions(instructions);
        mealPlan.setSavedRecipe(null);
        mealPlan.setUser(user);
        mealPlan.setIsBatch(false);
        mealPlan.setSourcePatternKey(sourcePatternKey);
        try {
            List<Map<String, Object>> normalized = normalizeIngredients(ingredients != null ? ingredients : new ArrayList<>());
            mealPlan.setIngredientsJson(objectMapper.writeValueAsString(normalized));
        } catch (Exception e) {
            mealPlan.setIngredientsJson("[]");
        }
        mealPlan = mealPlanRepository.save(mealPlan);
        return mapToDTO(mealPlan);
    }

    /**
     * Applies enabled week patterns to the given week: fills empty slots with suggested recipes.
     */
    @SuppressWarnings("unchecked")
    public List<MealPlanDTO> applyPatternsToWeek(Long userId, LocalDate weekStart) {
        MealPlanPreferences prefs = preferencesRepository.findByUser_Id(userId).orElse(null);
        if (prefs == null || prefs.getPatternsJson() == null || prefs.getPatternsJson().isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> patterns;
        try {
            patterns = objectMapper.readValue(prefs.getPatternsJson(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
        if (patterns == null || patterns.isEmpty()) return new ArrayList<>();

        LocalDate endDate = weekStart.plusDays(6);
        List<MealPlan> existing = mealPlanRepository.findWeeklyMealPlans(userId, weekStart, endDate);
        List<MealPlanDTO> created = new ArrayList<>();

        for (Map<String, Object> pattern : patterns) {
            String key = (String) pattern.get("key");
            String dietFilter = (String) pattern.get("dietFilter");
            Object dayObj = pattern.get("dayOfWeek");
            Object mealObj = pattern.get("mealType");
            if (key == null || dayObj == null || mealObj == null) continue;

            int dayOfWeek = dayObj instanceof Number ? ((Number) dayObj).intValue() : 1;
            if (dayOfWeek < 1 || dayOfWeek > 7) continue;
            LocalDate date = weekStart.plusDays(dayOfWeek - 1);
            List<MealPlan.MealType> mealTypes = mealTypesFromPattern(mealObj.toString());

            RecipeSuggestionRequest request = new RecipeSuggestionRequest();
            request.setDiet(dietFilter != null ? dietFilter : "ALL");
            request.setArea(null);
            request.setMealType(SavedRecipe.MealType.ALL);
            injectDietaryRulesFromPreferences(request, prefs);
            List<RecipeDTO> suggestions = recipeService.suggestRecipes(userId, request, 0);
            if (suggestions.isEmpty()) continue;

            RecipeDTO first = suggestions.get(0);
            for (MealPlan.MealType mealType : mealTypes) {
                boolean alreadyFilled = existing.stream().anyMatch(m -> m.getDate().equals(date) && m.getMealType() == mealType);
                if (alreadyFilled) continue;

                MealPlanDTO dto = createMealPlanFromRecipeDetails(
                    userId, date, mealType,
                    first.getRecipeName(), first.getIngredients(), first.getInstructions(),
                    first.getImageUrl(), first.getPrepTimeMinutes(), first.getCookTimeMinutes(), first.getServings(),
                    key
                );
                created.add(dto);
                MealPlan added = mealPlanRepository.findById(dto.getId()).orElse(null);
                if (added != null) existing.add(added);
            }
        }
        return created;
    }

    private void injectDietaryRulesFromPreferences(RecipeSuggestionRequest request, MealPlanPreferences prefs) {
        if (prefs == null || prefs.getDietaryRulesJson() == null || prefs.getDietaryRulesJson().isEmpty()) return;
        try {
            Map<String, Object> rules = objectMapper.readValue(prefs.getDietaryRulesJson(), new TypeReference<Map<String, Object>>() {});
            if (rules != null && !rules.isEmpty()) request.setDietaryRules(rules);
        } catch (Exception e) {
            // ignore
        }
    }

    /** Expands pattern meal type (B+L, L+D, B+D, All) to list of meal types. */
    private static List<MealPlan.MealType> mealTypesFromPattern(String s) {
        if (s == null || s.isEmpty()) return List.of(MealPlan.MealType.DINNER);
        switch (s.toUpperCase()) {
            case "BREAKFAST": return List.of(MealPlan.MealType.BREAKFAST);
            case "LUNCH": return List.of(MealPlan.MealType.LUNCH);
            case "DINNER": return List.of(MealPlan.MealType.DINNER);
            case "BREAKFAST_LUNCH":
            case "B+L": return List.of(MealPlan.MealType.BREAKFAST, MealPlan.MealType.LUNCH);
            case "LUNCH_DINNER":
            case "L+D": return List.of(MealPlan.MealType.LUNCH, MealPlan.MealType.DINNER);
            case "BREAKFAST_DINNER":
            case "B+D": return List.of(MealPlan.MealType.BREAKFAST, MealPlan.MealType.DINNER);
            case "ALL": return List.of(MealPlan.MealType.BREAKFAST, MealPlan.MealType.LUNCH, MealPlan.MealType.DINNER);
            default: return List.of(MealPlan.MealType.DINNER);
        }
    }

    /**
     * Reverts pattern-filled slots for the given week.
     */
    public int revertPatternsForWeek(Long userId, LocalDate weekStart) {
        LocalDate endDate = weekStart.plusDays(6);
        List<MealPlan> toRemove = mealPlanRepository.findWeeklyMealPlansWithSourcePattern(userId, weekStart, endDate);
        int count = toRemove.size();
        for (MealPlan m : toRemove) {
            mealPlanRepository.delete(m);
        }
        return count;
    }

    /**
     * Export week for share/PDF.
     */
    public WeekExportDTO getWeekExport(Long userId, LocalDate weekStart, LocalDate endDate, boolean includeShoppingSummary) {
        List<MealPlanDTO> plans = getWeeklyMealPlan(userId, weekStart, endDate);
        List<Map<String, Object>> shoppingSummary = null;
        if (includeShoppingSummary && !plans.isEmpty()) {
            shoppingSummary = aggregateIngredientsForExport(plans);
        }
        return new WeekExportDTO(weekStart, endDate, plans, shoppingSummary);
    }

    private List<Map<String, Object>> aggregateIngredientsForExport(List<MealPlanDTO> plans) {
        Map<String, Map<String, Object>> byKey = new HashMap<>();
        for (MealPlanDTO plan : plans) {
            if (plan.getIngredients() == null) continue;
            for (Map<String, Object> ing : plan.getIngredients()) {
                String name = String.valueOf(ing.get("name"));
                String unit = String.valueOf(ing.get("unit") != null ? ing.get("unit") : "pieces");
                double qty = ing.get("quantity") instanceof Number ? ((Number) ing.get("quantity")).doubleValue() : 1.0;
                String key = name.toLowerCase() + "|" + unit.toLowerCase();
                byKey.computeIfAbsent(key, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", name);
                    m.put("unit", unit);
                    m.put("quantity", 0.0);
                    return m;
                });
                Map<String, Object> m = byKey.get(key);
                m.put("quantity", ((Number) m.get("quantity")).doubleValue() + qty);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    /**
     * Suggests 1–2 slots where "Leftover" could be added (cook once, eat twice).
     */
    public List<Map<String, Object>> suggestLeftoverSlots(Long userId, Long mealPlanId) {
        MealPlan source = mealPlanRepository.findById(mealPlanId).orElseThrow(() -> new RuntimeException("Meal plan not found"));
        if (!source.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        LocalDate date = source.getDate();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        LocalDate nextDay = date.plusDays(1);
        // Suggest next day breakfast
        Map<String, Object> breakfast = new HashMap<>();
        breakfast.put("date", nextDay);
        breakfast.put("mealType", MealPlan.MealType.BREAKFAST.name());
        breakfast.put("label", "Next day breakfast");
        suggestions.add(breakfast);
        // Suggest next day lunch
        Map<String, Object> lunch = new HashMap<>();
        lunch.put("date", nextDay);
        lunch.put("mealType", MealPlan.MealType.LUNCH.name());
        lunch.put("label", "Next day lunch");
        suggestions.add(lunch);
        // Suggest next day dinner
        Map<String, Object> dinner = new HashMap<>();
        dinner.put("date", nextDay);
        dinner.put("mealType", MealPlan.MealType.DINNER.name());
        dinner.put("label", "Next day dinner");
        suggestions.add(dinner);
        return suggestions;
    }

    /**
     * Adds a "Leftover" entry for a batch-cooked meal to another slot.
     */
    public MealPlanDTO addLeftoverToSlot(Long userId, Long sourceMealPlanId, LocalDate date, MealPlan.MealType mealType) {
        MealPlan source = mealPlanRepository.findById(sourceMealPlanId).orElseThrow(() -> new RuntimeException("Meal plan not found"));
        if (!source.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        if (mealPlanRepository.findByUserIdAndDateAndMealType(userId, date, mealType).isPresent()) {
            throw new RuntimeException("Slot already filled");
        }
        String recipeName = "Leftover: " + source.getRecipeName();
        MealPlanDTO sourceDto = mapToDTO(source);
        List<Map<String, Object>> ingredients = sourceDto.getIngredients() != null ? sourceDto.getIngredients() : new ArrayList<>();
        MealPlanDTO created = createMealPlanFromRecipeDetails(userId, date, mealType, recipeName, ingredients,
            source.getInstructions(), source.getImageUrl(), 0, 0, source.getServings(), null);
        setLeftoverSource(userId, created.getId(), sourceMealPlanId);
        return getMealPlanById(userId, created.getId());
    }

    private MealPlanDTO getMealPlanById(Long userId, Long id) {
        MealPlan m = mealPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("Meal plan not found"));
        if (!m.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        return mapToDTO(m);
    }

    /** Called after createMealPlanFromRecipeDetails for leftover: set leftoverOfMealPlan. */
    public void setLeftoverSource(Long userId, Long mealPlanId, Long sourceMealPlanId) {
        MealPlan plan = mealPlanRepository.findById(mealPlanId).orElseThrow(() -> new RuntimeException("Meal plan not found"));
        if (!plan.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        MealPlan source = mealPlanRepository.findById(sourceMealPlanId).orElseThrow(() -> new RuntimeException("Source not found"));
        plan.setLeftoverOfMealPlan(source);
        mealPlanRepository.save(plan);
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
        dto.setIsBatch(mealPlan.getIsBatch() != null ? mealPlan.getIsBatch() : false);
        dto.setLeftoverOfMealPlanId(mealPlan.getLeftoverOfMealPlan() != null ? mealPlan.getLeftoverOfMealPlan().getId() : null);
        dto.setSourcePatternKey(mealPlan.getSourcePatternKey());

        // Parse ingredients JSON - support name, quantity/requiredQuantity/amount, unit/requiredUnit
        try {
            if (mealPlan.getIngredientsJson() != null) {
                JsonNode ingredientsJson = objectMapper.readTree(mealPlan.getIngredientsJson());
                List<Map<String, Object>> ingredients = new ArrayList<>();
                for (JsonNode ing : ingredientsJson) {
                    JsonNode nameNode = ing.path("name");
                    if (nameNode.isMissingNode() || nameNode.asText().trim().isEmpty()) continue;
                    double qty = 0;
                    if (!ing.path("quantity").isMissingNode()) qty = ing.path("quantity").asDouble();
                    else if (!ing.path("requiredQuantity").isMissingNode()) qty = ing.path("requiredQuantity").asDouble();
                    else if (!ing.path("amount").isMissingNode()) qty = ing.path("amount").asDouble();
                    String unit = "pieces";
                    if (!ing.path("unit").isMissingNode() && !ing.path("unit").asText().trim().isEmpty())
                        unit = ing.path("unit").asText().trim();
                    else if (!ing.path("requiredUnit").isMissingNode() && !ing.path("requiredUnit").asText().trim().isEmpty())
                        unit = ing.path("requiredUnit").asText().trim();
                    Map<String, Object> ingredient = new HashMap<>();
                    ingredient.put("name", nameNode.asText().trim());
                    ingredient.put("quantity", qty);
                    ingredient.put("unit", unit);
                    ingredients.add(ingredient);
                }
                dto.setIngredients(ingredients);
            }
        } catch (Exception e) {
            dto.setIngredients(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Normalizes ingredient maps to canonical format: name, quantity, unit.
     * Supports requiredQuantity/amount and requiredUnit from Spoonacular/enhanced recipes.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeIngredients(List<?> raw) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object o : raw) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) o;
            Object nameObj = m.get("name");
            if (nameObj == null || nameObj.toString().trim().isEmpty()) continue;
            double qty = 0;
            Object qObj = m.get("quantity");
            if (qObj == null) qObj = m.get("requiredQuantity");
            if (qObj == null) qObj = m.get("amount");
            if (qObj != null) {
                try { qty = Double.parseDouble(qObj.toString()); } catch (NumberFormatException ignored) {}
            }
            Object uObj = m.get("unit");
            if (uObj == null) uObj = m.get("requiredUnit");
            String unit = (uObj != null && !uObj.toString().trim().isEmpty()) ? uObj.toString().trim() : "pieces";
            Map<String, Object> norm = new HashMap<>();
            norm.put("name", nameObj.toString().trim());
            norm.put("quantity", qty);
            norm.put("unit", unit);
            result.add(norm);
        }
        return result;
    }
}
