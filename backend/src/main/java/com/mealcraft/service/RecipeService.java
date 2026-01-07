package com.mealcraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.RecipeDTO;
import com.mealcraft.dto.RecipeSuggestionRequest;
import com.mealcraft.model.PantryItem;
import com.mealcraft.model.SavedRecipe;
import com.mealcraft.model.User;
import com.mealcraft.repository.PantryItemRepository;
import com.mealcraft.repository.SavedRecipeRepository;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Recipe Service
 * 
 * Handles recipe suggestion and saved recipe management.
 * Integrates with TheMealDB API for recipe suggestions based on pantry ingredients.
 * Uses parallel API calls and caching for optimal performance.
 * Manages user's saved recipe collection.
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class RecipeService {

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${themealdb.api.base-url:https://www.themealdb.com/api/json/v1/1}")
    private String themealdbBaseUrl;

    // In-memory cache for recipe details (recipeId -> RecipeDTO)
    // Cache expires after 1 hour
    private final Map<String, RecipeDTO> recipeCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1);

    // Cache for ingredient search results (ingredientName -> List of recipe IDs)
    private final Map<String, List<String>> ingredientSearchCache = new ConcurrentHashMap<>();
    private final Map<String, Long> ingredientCacheTimestamps = new ConcurrentHashMap<>();
    private static final long INGREDIENT_CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(6);

    /**
     * Suggests recipes based on pantry ingredients
     * 
     * Uses parallel API calls to search recipes by each ingredient,
     * finds recipes that match multiple ingredients (intersection),
     * and calculates match percentages.
     * 
     * @param userId User's ID
     * @param request Recipe suggestion request with meal type filter
     * @return List of suggested RecipeDTO sorted by relevance
     */
    public List<RecipeDTO> suggestRecipes(Long userId, RecipeSuggestionRequest request) {
        try {
            // Get user's pantry items
            List<PantryItem> pantryItems = pantryItemRepository.findByUserId(userId);
            
            if (pantryItems.isEmpty()) {
                return new ArrayList<>();
            }

            // Filter out expired items for better suggestions (optional - can be changed)
            List<PantryItem> validPantryItems = pantryItems.stream()
                .filter(item -> item.getDaysUntilExpiry() >= -1) // Include items expiring today
                .collect(Collectors.toList());

            if (validPantryItems.isEmpty()) {
                validPantryItems = pantryItems; // Use all items if none are valid
            }

            // Step 1: Search recipes by each ingredient in parallel
            Map<String, List<String>> ingredientRecipeMap = searchRecipesByIngredientsParallel(validPantryItems);

            if (ingredientRecipeMap.isEmpty()) {
                return new ArrayList<>();
            }

            // Step 2: Find recipes that match multiple ingredients (intersection)
            Map<String, Integer> recipeMatchCount = findRecipeIntersections(ingredientRecipeMap);

            if (recipeMatchCount.isEmpty()) {
                return new ArrayList<>();
            }

            // Step 3: Sort recipes by match count (recipes with more matching ingredients first)
            List<String> sortedRecipeIds = recipeMatchCount.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(30) // Limit to top 30 recipes for efficiency
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            // Step 4: Fetch recipe details in parallel
            List<RecipeDTO> suggestedRecipes = fetchRecipeDetailsParallel(sortedRecipeIds, validPantryItems, request);

            // Step 5: Sort by priority (expiring ingredients first) and match percentage
            suggestedRecipes.sort((r1, r2) -> {
                boolean r1Expiring = r1.getUsesExpiringIngredients();
                boolean r2Expiring = r2.getUsesExpiringIngredients();
                if (r1Expiring != r2Expiring) {
                    return r2Expiring ? 1 : -1; // Expiring first
                }
                return Double.compare(r2.getMatchPercentage(), r1.getMatchPercentage());
            });

            return suggestedRecipes;

        } catch (Exception e) {
            System.err.println("Error fetching recipes from TheMealDB API: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Searches recipes by each ingredient in parallel
     * 
     * @param pantryItems List of pantry items
     * @return Map of ingredient name to list of recipe IDs
     */
    private Map<String, List<String>> searchRecipesByIngredientsParallel(List<PantryItem> pantryItems) {
        // Create parallel tasks for each ingredient search
        List<CompletableFuture<AbstractMap.SimpleEntry<String, List<String>>>> futures = pantryItems.stream()
            .map(item -> CompletableFuture.supplyAsync(() -> {
                String ingredientName = normalizeIngredientName(item.getItemName());
                List<String> recipeIds = searchRecipesByIngredient(ingredientName);
                return new AbstractMap.SimpleEntry<>(item.getItemName(), recipeIds);
            }))
            .collect(Collectors.toList());

        // Wait for all tasks to complete and collect results
        Map<String, List<String>> result = new HashMap<>();
        for (CompletableFuture<AbstractMap.SimpleEntry<String, List<String>>> future : futures) {
            try {
                AbstractMap.SimpleEntry<String, List<String>> entry = future.get(5, TimeUnit.SECONDS);
                if (entry != null && !entry.getValue().isEmpty()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                System.err.println("Error in parallel ingredient search: " + e.getMessage());
                // Continue with other ingredients even if one fails
            }
        }

        return result;
    }

    /**
     * Searches recipes by a single ingredient using TheMealDB API
     * Uses caching to avoid redundant API calls
     * 
     * @param ingredientName Normalized ingredient name
     * @return List of recipe IDs
     */
    private List<String> searchRecipesByIngredient(String ingredientName) {
        // Check cache first
        String cacheKey = ingredientName.toLowerCase();
        if (ingredientSearchCache.containsKey(cacheKey)) {
            Long timestamp = ingredientCacheTimestamps.get(cacheKey);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) < INGREDIENT_CACHE_EXPIRY_MS) {
                return new ArrayList<>(ingredientSearchCache.get(cacheKey));
            }
        }

        try {
            String url = String.format("%s/filter.php?i=%s",
                themealdbBaseUrl,
                java.net.URLEncoder.encode(ingredientName, java.nio.charset.StandardCharsets.UTF_8));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            List<String> recipeIds = new ArrayList<>();
            JsonNode meals = jsonResponse.path("meals");
            
            if (meals != null && meals.isArray()) {
                for (JsonNode meal : meals) {
                    String id = meal.path("idMeal").asText();
                    if (id != null && !id.isEmpty()) {
                        recipeIds.add(id);
                    }
                }
            }

            // Cache the results
            ingredientSearchCache.put(cacheKey, recipeIds);
            ingredientCacheTimestamps.put(cacheKey, System.currentTimeMillis());

            return recipeIds;

        } catch (RestClientException e) {
            System.err.println("Error calling TheMealDB API for ingredient: " + ingredientName + " - " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error parsing TheMealDB response for ingredient: " + ingredientName + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Finds recipes that match multiple ingredients (intersection)
     * 
     * @param ingredientRecipeMap Map of ingredient to recipe IDs
     * @return Map of recipe ID to match count
     */
    private Map<String, Integer> findRecipeIntersections(Map<String, List<String>> ingredientRecipeMap) {
        Map<String, Integer> recipeMatchCount = new HashMap<>();

        // Count how many ingredients each recipe matches
        for (List<String> recipeIds : ingredientRecipeMap.values()) {
            for (String recipeId : recipeIds) {
                recipeMatchCount.put(recipeId, recipeMatchCount.getOrDefault(recipeId, 0) + 1);
            }
        }

        // Filter recipes that match at least 2 ingredients (or 1 if only 1 ingredient in pantry)
        int minMatches = ingredientRecipeMap.size() > 1 ? 2 : 1;
        return recipeMatchCount.entrySet().stream()
            .filter(entry -> entry.getValue() >= minMatches)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Fetches recipe details in parallel for multiple recipe IDs
     * 
     * @param recipeIds List of recipe IDs
     * @param pantryItems User's pantry items
     * @param request Recipe suggestion request with meal type filter
     * @return List of RecipeDTO
     */
    private List<RecipeDTO> fetchRecipeDetailsParallel(List<String> recipeIds, 
                                                       List<PantryItem> pantryItems,
                                                       RecipeSuggestionRequest request) {
        // Create parallel tasks for fetching recipe details
        List<CompletableFuture<RecipeDTO>> futures = recipeIds.stream()
            .map(recipeId -> CompletableFuture.supplyAsync(() -> {
                RecipeDTO recipe = fetchRecipeDetails(recipeId);
                if (recipe != null) {
                    // Calculate match percentage
                    recipe.setMatchPercentage(calculateMatchPercentage(recipe, pantryItems));
                    
                    // Check for expiring ingredients
                    LocalDate today = LocalDate.now();
                    List<Map<String, Object>> expiringIngredients = findExpiringIngredients(
                        recipe, pantryItems, today);
                    recipe.setExpiringIngredients(expiringIngredients);
                    recipe.setUsesExpiringIngredients(!expiringIngredients.isEmpty());
                }
                return recipe;
            }))
            .collect(Collectors.toList());

        // Wait for all tasks and collect results
        List<RecipeDTO> recipes = new ArrayList<>();
        for (CompletableFuture<RecipeDTO> future : futures) {
            try {
                RecipeDTO recipe = future.get(5, TimeUnit.SECONDS);
                if (recipe != null) {
                    // Apply meal type filter
                    if (request.getMealType() == SavedRecipe.MealType.ALL || 
                        recipe.getMealType() == request.getMealType()) {
                        recipes.add(recipe);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching recipe details in parallel: " + e.getMessage());
                // Continue with other recipes even if one fails
            }
        }

        return recipes;
    }

    /**
     * Fetches detailed recipe information from TheMealDB API
     * Uses caching to avoid redundant API calls
     * 
     * @param recipeId TheMealDB recipe ID
     * @return RecipeDTO with full details, or null if not found
     */
    private RecipeDTO fetchRecipeDetails(String recipeId) {
        // Check cache first
        if (recipeCache.containsKey(recipeId)) {
            Long timestamp = cacheTimestamps.get(recipeId);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
                return recipeCache.get(recipeId);
            }
        }

        try {
            String url = String.format("%s/lookup.php?i=%s", themealdbBaseUrl, recipeId);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            JsonNode meals = jsonResponse.path("meals");
            if (meals == null || !meals.isArray() || meals.size() == 0) {
                return null;
            }

            JsonNode recipeJson = meals.get(0);
            RecipeDTO recipeDTO = parseTheMealDBRecipe(recipeJson);

            // Cache the recipe
            if (recipeDTO != null) {
                recipeCache.put(recipeId, recipeDTO);
                cacheTimestamps.put(recipeId, System.currentTimeMillis());
            }

            return recipeDTO;

        } catch (RestClientException e) {
            System.err.println("Error calling TheMealDB API for recipe: " + recipeId + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing TheMealDB recipe: " + recipeId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses TheMealDB API recipe response into RecipeDTO
     * 
     * TheMealDB uses a specific format with strIngredient1, strIngredient2, etc.
     * and strMeasure1, strMeasure2, etc. for ingredients.
     * 
     * @param recipeJson TheMealDB recipe JSON node
     * @return RecipeDTO with full details
     */
    private RecipeDTO parseTheMealDBRecipe(JsonNode recipeJson) {
        try {
            RecipeDTO recipeDTO = new RecipeDTO();
            
            // Recipe ID
            String recipeId = recipeJson.path("idMeal").asText("");
            if (!recipeId.isEmpty()) {
                try {
                    recipeDTO.setExternalRecipeId(Long.parseLong(recipeId));
                } catch (NumberFormatException e) {
                    recipeDTO.setExternalRecipeId((long) recipeId.hashCode());
                }
            }
            
            // Recipe name
            recipeDTO.setRecipeName(recipeJson.path("strMeal").asText("Unknown Recipe"));
            
            // Image URL
            recipeDTO.setImageUrl(recipeJson.path("strMealThumb").asText(""));
            
            // Category (used to infer meal type)
            String category = recipeJson.path("strCategory").asText("").toUpperCase();
            
            // TheMealDB doesn't provide prep/cook time, so we'll estimate based on category
            int estimatedTime = estimateCookingTime(category);
            recipeDTO.setPrepTimeMinutes(estimatedTime / 3); // Rough estimate: 1/3 prep, 2/3 cook
            recipeDTO.setCookTimeMinutes(estimatedTime * 2 / 3);
            
            // Servings (TheMealDB doesn't provide this, default to 4)
            recipeDTO.setServings(4);

            // Parse ingredients
            // TheMealDB uses strIngredient1, strIngredient2, ..., strIngredient20
            // and strMeasure1, strMeasure2, ..., strMeasure20
            List<Map<String, Object>> ingredients = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String ingredientName = recipeJson.path("strIngredient" + i).asText("").trim();
                String measure = recipeJson.path("strMeasure" + i).asText("").trim();
                
                if (ingredientName != null && !ingredientName.isEmpty() && !ingredientName.equals("null")) {
                    Map<String, Object> ing = new HashMap<>();
                    ing.put("name", ingredientName);
                    
                    // Parse quantity from measure (e.g., "1 cup" -> quantity: 1, unit: "cup")
                    Map<String, Object> parsedMeasure = parseMeasure(measure);
                    ing.put("quantity", parsedMeasure.get("quantity"));
                    ing.put("unit", parsedMeasure.get("unit"));
                    
                    ingredients.add(ing);
                }
            }
            recipeDTO.setIngredients(ingredients);

            // Parse instructions
            String instructions = recipeJson.path("strInstructions").asText("");
            if (instructions != null && !instructions.isEmpty()) {
                // TheMealDB instructions are usually in paragraph format
                // Split by newlines or periods and number them
                String[] steps = instructions.split("\r\n|\n|\\. ");
                StringBuilder formattedInstructions = new StringBuilder();
                int stepNumber = 1;
                for (String step : steps) {
                    step = step.trim();
                    if (!step.isEmpty()) {
                        formattedInstructions.append(stepNumber++)
                            .append(". ")
                            .append(step);
                        if (!step.endsWith(".")) {
                            formattedInstructions.append(".");
                        }
                        formattedInstructions.append("\n");
                    }
                }
                recipeDTO.setInstructions(formattedInstructions.toString());
            } else {
                recipeDTO.setInstructions("Instructions not available.");
            }

            // Determine meal type based on category
            SavedRecipe.MealType mealType = determineMealType(category);
            recipeDTO.setMealType(mealType);

            return recipeDTO;

        } catch (Exception e) {
            System.err.println("Error parsing TheMealDB recipe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Estimates cooking time based on recipe category
     * 
     * @param category Recipe category
     * @return Estimated total time in minutes
     */
    private int estimateCookingTime(String category) {
        category = category.toUpperCase();
        if (category.contains("BREAKFAST")) {
            return 15; // Quick breakfast
        } else if (category.contains("DESSERT")) {
            return 45; // Desserts take time
        } else if (category.contains("SEAFOOD")) {
            return 30; // Seafood is usually quick
        } else if (category.contains("BEEF") || category.contains("LAMB")) {
            return 60; // Meat dishes take longer
        } else {
            return 40; // Default estimate
        }
    }

    /**
     * Parses measure string to extract quantity and unit
     * 
     * @param measure Measure string (e.g., "1 cup", "2 tablespoons", "500g")
     * @return Map with "quantity" and "unit"
     */
    private Map<String, Object> parseMeasure(String measure) {
        Map<String, Object> result = new HashMap<>();
        
        if (measure == null || measure.trim().isEmpty()) {
            result.put("quantity", 1.0);
            result.put("unit", "piece");
            return result;
        }

        measure = measure.trim();
        
        // Try to extract number from the beginning
        String[] parts = measure.split("\\s+", 2);
        try {
            double quantity = Double.parseDouble(parts[0]);
            result.put("quantity", quantity);
            if (parts.length > 1) {
                result.put("unit", parts[1]);
            } else {
                result.put("unit", "");
            }
        } catch (NumberFormatException e) {
            // If no number found, assume quantity 1
            result.put("quantity", 1.0);
            result.put("unit", measure);
        }
        
        return result;
    }

    /**
     * Determines meal type based on category
     * 
     * @param category Recipe category
     * @return MealType enum
     */
    private SavedRecipe.MealType determineMealType(String category) {
        category = category.toUpperCase();
        if (category.contains("BREAKFAST")) {
            return SavedRecipe.MealType.BREAKFAST;
        } else if (category.contains("LUNCH")) {
            return SavedRecipe.MealType.LUNCH;
        } else if (category.contains("DINNER") || category.contains("MAIN")) {
            return SavedRecipe.MealType.DINNER;
        } else {
            return SavedRecipe.MealType.ALL;
        }
    }

    /**
     * Normalizes ingredient name for better matching
     * Handles plurals, common variations, and removes extra whitespace
     * 
     * @param ingredientName Original ingredient name
     * @return Normalized ingredient name
     */
    private String normalizeIngredientName(String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            return "";
        }

        String normalized = ingredientName.trim().toLowerCase();

        // Remove common prefixes/suffixes
        normalized = normalized.replaceAll("^(fresh|dried|frozen|ground|chopped|sliced|diced)\\s+", "");
        normalized = normalized.replaceAll("\\s+(fresh|dried|frozen|ground|chopped|sliced|diced)$", "");

        // Handle common plurals (simple approach)
        if (normalized.endsWith("ies")) {
            normalized = normalized.substring(0, normalized.length() - 3) + "y";
        } else if (normalized.endsWith("es") && !normalized.endsWith("ches") && !normalized.endsWith("shes")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("s") && !normalized.endsWith("ss")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // Handle common variations
        Map<String, String> variations = new HashMap<>();
        variations.put("tomato", "tomato");
        variations.put("tomatoes", "tomato");
        variations.put("onion", "onion");
        variations.put("onions", "onion");
        variations.put("garlic", "garlic");
        variations.put("garlic clove", "garlic");
        variations.put("chicken breast", "chicken");
        variations.put("chicken thighs", "chicken");
        variations.put("chicken", "chicken");
        variations.put("beef", "beef");
        variations.put("ground beef", "beef");
        variations.put("milk", "milk");
        variations.put("whole milk", "milk");
        variations.put("eggs", "egg");
        variations.put("egg", "egg");

        return variations.getOrDefault(normalized, normalized);
    }

    /**
     * Calculates match percentage (how many ingredients user has)
     * Uses normalized ingredient names for better matching
     * 
     * @param recipe RecipeDTO
     * @param pantryItems User's pantry items
     * @return Match percentage (0-100)
     */
    private Double calculateMatchPercentage(RecipeDTO recipe, List<PantryItem> pantryItems) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return 0.0;
        }

        // Create normalized set of pantry item names
        Set<String> pantryItemNames = pantryItems.stream()
            .map(item -> normalizeIngredientName(item.getItemName()))
            .collect(Collectors.toSet());

        // Count matched ingredients (using normalized names)
        long matchedIngredients = recipe.getIngredients().stream()
            .map(ing -> {
                String name = ing.get("name").toString();
                return normalizeIngredientName(name);
            })
            .filter(pantryItemNames::contains)
            .count();

        if (recipe.getIngredients().size() == 0) {
            return 0.0;
        }

        return (matchedIngredients * 100.0) / recipe.getIngredients().size();
    }

    /**
     * Finds expiring ingredients used in recipe
     * Uses normalized ingredient names for matching
     * 
     * @param recipe RecipeDTO
     * @param pantryItems User's pantry items
     * @param today Current date
     * @return List of expiring ingredients with days until expiry
     */
    private List<Map<String, Object>> findExpiringIngredients(RecipeDTO recipe, 
                                                               List<PantryItem> pantryItems,
                                                               LocalDate today) {
        List<Map<String, Object>> expiringIngredients = new ArrayList<>();
        
        if (recipe.getIngredients() == null) {
            return expiringIngredients;
        }

        // Create map of normalized pantry item names to PantryItem
        Map<String, PantryItem> pantryMap = new HashMap<>();
        for (PantryItem item : pantryItems) {
            String normalized = normalizeIngredientName(item.getItemName());
            pantryMap.put(normalized, item);
        }

        // Check each recipe ingredient against pantry
        for (Map<String, Object> ingredient : recipe.getIngredients()) {
            String ingredientName = ingredient.get("name").toString();
            String normalized = normalizeIngredientName(ingredientName);
            
            PantryItem pantryItem = pantryMap.get(normalized);
            
            if (pantryItem != null) {
                long daysUntilExpiry = pantryItem.getDaysUntilExpiry();
                if (daysUntilExpiry >= 0 && daysUntilExpiry <= 5) {
                    Map<String, Object> expiringIng = new HashMap<>();
                    expiringIng.put("name", pantryItem.getItemName());
                    expiringIng.put("daysUntilExpiry", daysUntilExpiry);
                    expiringIngredients.add(expiringIng);
                }
            }
        }

        return expiringIngredients;
    }

    /**
     * Saves a recipe to user's collection
     * 
     * @param userId User's ID
     * @param recipeDTO Recipe to save
     * @return Saved RecipeDTO
     */
    public RecipeDTO saveRecipe(Long userId, RecipeDTO recipeDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        SavedRecipe savedRecipe = new SavedRecipe();
        savedRecipe.setRecipeName(recipeDTO.getRecipeName());
        savedRecipe.setMealType(recipeDTO.getMealType());
        savedRecipe.setImageUrl(recipeDTO.getImageUrl());
        savedRecipe.setPrepTimeMinutes(recipeDTO.getPrepTimeMinutes());
        savedRecipe.setCookTimeMinutes(recipeDTO.getCookTimeMinutes());
        savedRecipe.setServings(recipeDTO.getServings());
        savedRecipe.setInstructions(recipeDTO.getInstructions());
        savedRecipe.setNotes(recipeDTO.getNotes());
        savedRecipe.setMatchPercentage(recipeDTO.getMatchPercentage());
        savedRecipe.setExternalRecipeId(recipeDTO.getExternalRecipeId());
        savedRecipe.setUser(user);

        // Convert ingredients to JSON
        try {
            String ingredientsJson = objectMapper.writeValueAsString(recipeDTO.getIngredients());
            savedRecipe.setIngredientsJson(ingredientsJson);
        } catch (Exception e) {
            savedRecipe.setIngredientsJson("[]");
        }

        savedRecipe = savedRecipeRepository.save(savedRecipe);
        return mapToDTO(savedRecipe);
    }

    /**
     * Gets all saved recipes for a user
     * 
     * @param userId User's ID
     * @return List of saved RecipeDTO
     */
    public List<RecipeDTO> getSavedRecipes(Long userId) {
        List<SavedRecipe> recipes = savedRecipeRepository.findByUserId(userId);
        return recipes.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Searches saved recipes by name
     * 
     * @param userId User's ID
     * @param searchQuery Search query
     * @return List of matching RecipeDTO
     */
    public List<RecipeDTO> searchSavedRecipes(Long userId, String searchQuery) {
        List<SavedRecipe> recipes = savedRecipeRepository.searchByRecipeName(userId, searchQuery);
        return recipes.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets saved recipe by ID
     * 
     * @param userId User's ID
     * @param recipeId Recipe ID
     * @return RecipeDTO
     */
    public RecipeDTO getSavedRecipe(Long userId, Long recipeId) {
        SavedRecipe recipe = savedRecipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Recipe does not belong to user");
        }

        return mapToDTO(recipe);
    }

    /**
     * Updates a saved recipe
     * 
     * @param userId User's ID
     * @param recipeId Recipe ID
     * @param recipeDTO Updated recipe data
     * @return Updated RecipeDTO
     */
    public RecipeDTO updateSavedRecipe(Long userId, Long recipeId, RecipeDTO recipeDTO) {
        SavedRecipe recipe = savedRecipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Recipe does not belong to user");
        }

        // Update fields
        recipe.setRecipeName(recipeDTO.getRecipeName());
        recipe.setMealType(recipeDTO.getMealType());
        recipe.setImageUrl(recipeDTO.getImageUrl());
        recipe.setPrepTimeMinutes(recipeDTO.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(recipeDTO.getCookTimeMinutes());
        recipe.setServings(recipeDTO.getServings());
        recipe.setInstructions(recipeDTO.getInstructions());
        recipe.setNotes(recipeDTO.getNotes());

        // Update ingredients JSON
        try {
            String ingredientsJson = objectMapper.writeValueAsString(recipeDTO.getIngredients());
            recipe.setIngredientsJson(ingredientsJson);
        } catch (Exception e) {
            // Keep existing ingredients if conversion fails
        }

        recipe = savedRecipeRepository.save(recipe);
        return mapToDTO(recipe);
    }

    /**
     * Deletes a saved recipe
     * 
     * @param userId User's ID
     * @param recipeId Recipe ID
     */
    public void deleteSavedRecipe(Long userId, Long recipeId) {
        SavedRecipe recipe = savedRecipeRepository.findById(recipeId)
            .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Recipe does not belong to user");
        }

        savedRecipeRepository.delete(recipe);
    }

    /**
     * Maps SavedRecipe entity to RecipeDTO
     * 
     * @param recipe SavedRecipe entity
     * @return RecipeDTO
     */
    private RecipeDTO mapToDTO(SavedRecipe recipe) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setRecipeName(recipe.getRecipeName());
        dto.setMealType(recipe.getMealType());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setPrepTimeMinutes(recipe.getPrepTimeMinutes());
        dto.setCookTimeMinutes(recipe.getCookTimeMinutes());
        dto.setServings(recipe.getServings());
        dto.setInstructions(recipe.getInstructions());
        dto.setNotes(recipe.getNotes());
        dto.setMatchPercentage(recipe.getMatchPercentage());
        dto.setExternalRecipeId(recipe.getExternalRecipeId());

        // Parse ingredients JSON
        try {
            if (recipe.getIngredientsJson() != null) {
                JsonNode ingredientsJson = objectMapper.readTree(recipe.getIngredientsJson());
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
