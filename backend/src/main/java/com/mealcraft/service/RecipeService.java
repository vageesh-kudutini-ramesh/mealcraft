package com.mealcraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.RecipeDTO;
import com.mealcraft.dto.RecipeSuggestionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mealcraft.model.PantryItem;
import com.mealcraft.model.SavedRecipe;
import com.mealcraft.model.User;
import com.mealcraft.repository.MealPlanPreferencesRepository;
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
 * Uses Spoonacular API when configured (spoonacular.api.key in application.properties).
 * Efficient: 1 API call for suggest (findByIngredients or complexSearch), cached recipe details.
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

    @Autowired
    private UnitConversionService unitConversionService;

    @Autowired
    private MealPlanPreferencesRepository preferencesRepository;

    @Autowired(required = false)
    private SpoonacularClient spoonacularClient;

    @Value("${themealdb.api.base-url:https://www.themealdb.com/api/json/v1/1}")
    private String themealdbBaseUrl;

    private String getThemealdbBaseUrlNormalized() {
        if (themealdbBaseUrl == null) return "https://www.themealdb.com/api/json/v1/1";
        String base = themealdbBaseUrl.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    // In-memory cache for recipe details (recipeId -> RecipeDTO) - used when Spoonacular not configured
    private final Map<String, RecipeDTO> recipeCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1);

    // Cache for ingredient search (TheMealDB fallback only)
    private final Map<String, List<String>> ingredientSearchCache = new ConcurrentHashMap<>();
    private final Map<String, Long> ingredientCacheTimestamps = new ConcurrentHashMap<>();
    private static final long INGREDIENT_CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(6);

    /**
     * Suggests recipes based on pantry ingredients.
     * Uses Spoonacular when configured (1 API call, cached); otherwise fallback to TheMealDB.
     */
    public List<RecipeDTO> suggestRecipes(Long userId, RecipeSuggestionRequest request, int offset) {
        try {
            if (request.getDietaryRules() == null && preferencesRepository != null) {
                preferencesRepository.findByUser_Id(userId).ifPresent(prefs -> {
                    if (prefs.getDietaryRulesJson() != null && !prefs.getDietaryRulesJson().isEmpty()) {
                        try {
                            Map<String, Object> rules = objectMapper.readValue(prefs.getDietaryRulesJson(), new TypeReference<Map<String, Object>>() {});
                            if (rules != null && !rules.isEmpty()) request.setDietaryRules(rules);
                        } catch (Exception ignored) {}
                    }
                });
            }
            List<PantryItem> pantryItems = pantryItemRepository.findByUserId(userId);
            if (pantryItems.isEmpty()) {
                if (spoonacularClient != null && spoonacularClient.isConfigured()) {
                    return discoverRecipes(null, request.getArea(), deriveDiet(request), request.getDietaryRules(), offset);
                }
                return new ArrayList<>();
            }

            List<PantryItem> validPantryItems = pantryItems.stream()
                .filter(item -> item.getDaysUntilExpiry() >= -1)
                .collect(Collectors.toList());
            if (validPantryItems.isEmpty()) validPantryItems = pantryItems;

            if (spoonacularClient != null && spoonacularClient.isConfigured()) {
                List<RecipeDTO> spoonacular = suggestRecipesSpoonacular(validPantryItems, request, offset);
                if (!spoonacular.isEmpty()) return spoonacular;
                System.out.println("[RecipeService] Spoonacular returned empty, falling back to TheMealDB");
                return suggestRecipesTheMealDB(validPantryItems, request);
            }
            return suggestRecipesTheMealDB(validPantryItems, request);
        } catch (Exception e) {
            System.err.println("Error suggesting recipes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** Spoonacular path: 1 API call, map to RecipeDTO, apply meal type filter. */
    private List<RecipeDTO> suggestRecipesSpoonacular(List<PantryItem> pantryItems, RecipeSuggestionRequest request, int offset) {
        List<String> ingredientNames = pantryItems.stream()
            .map(item -> normalizeIngredientName(item.getItemName()))
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        if (ingredientNames.isEmpty()) return new ArrayList<>();

        String diet = deriveDiet(request);
        String intolerances = buildIntolerancesFromRules(request.getDietaryRules());
        String cuisine = request.getArea() != null ? request.getArea().trim() : "";

        List<Map<String, Object>> results = spoonacularClient.searchByIngredients(ingredientNames, diet, intolerances, cuisine, offset);
        List<RecipeDTO> dtos = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Map<String, Object> r : results) {
            RecipeDTO dto = mapSpoonacularSearchToDTO(r);
            if (dto == null) continue;
            if (request.getMealType() != null && request.getMealType() != SavedRecipe.MealType.ALL
                && dto.getMealType() != SavedRecipe.MealType.ALL && dto.getMealType() != request.getMealType()) {
                continue; // meal type filter - only when DTO has specific type; Spoonacular defaults to ALL so we include
            }
            if (request.getDietaryRules() != null && !request.getDietaryRules().isEmpty()
                && !passesDietaryRules(dto, request.getDietaryRules())) {
                continue;
            }
            dto.setMatchPercentage((Double) r.getOrDefault("matchPercentage", 85.0));
            List<Map<String, Object>> expiring = findExpiringIngredients(dto, pantryItems, today);
            dto.setExpiringIngredients(expiring);
            dto.setUsesExpiringIngredients(!expiring.isEmpty());
            dtos.add(dto);
        }

        dtos.sort((r1, r2) -> {
            if (Boolean.TRUE.equals(r1.getUsesExpiringIngredients()) != Boolean.TRUE.equals(r2.getUsesExpiringIngredients())) {
                return Boolean.TRUE.equals(r2.getUsesExpiringIngredients()) ? 1 : -1;
            }
            return Double.compare(
                r2.getMatchPercentage() != null ? r2.getMatchPercentage() : 0,
                r1.getMatchPercentage() != null ? r1.getMatchPercentage() : 0);
        });
        return dtos;
    }

    private String deriveDiet(RecipeSuggestionRequest request) {
        Map<String, Object> rules = request.getDietaryRules();
        if (rules != null && Boolean.TRUE.equals(rules.get("VEGAN_ONLY"))) {
            return "VEGAN";
        }
        return request.getDiet() != null ? request.getDiet() : "ALL";
    }

    private String buildIntolerancesFromRules(Map<String, Object> rules) {
        if (rules == null) return "";
        List<String> parts = new ArrayList<>();
        if (Boolean.TRUE.equals(rules.get("NO_GLUTEN"))) parts.add("gluten");
        if (Boolean.TRUE.equals(rules.get("NO_DAIRY"))) parts.add("dairy");
        return String.join(",", parts);
    }

    private RecipeDTO mapSpoonacularSearchToDTO(Map<String, Object> r) {
        if (r == null) return null;
        RecipeDTO dto = new RecipeDTO();
        Object idObj = r.get("id");
        if (idObj instanceof Number) dto.setExternalRecipeId(((Number) idObj).longValue());
        dto.setRecipeName(r.get("title") != null ? r.get("title").toString() : "Unknown");
        dto.setImageUrl(r.get("image") != null ? r.get("image").toString() : "");
        int ready = r.get("readyInMinutes") instanceof Number ? ((Number) r.get("readyInMinutes")).intValue() : 30;
        dto.setPrepTimeMinutes(ready / 3);
        dto.setCookTimeMinutes(ready * 2 / 3);
        dto.setServings(r.get("servings") instanceof Number ? ((Number) r.get("servings")).intValue() : 4);
        dto.setMealType(SavedRecipe.MealType.ALL);
        dto.setCategory("");
        dto.setIngredients(new ArrayList<>());
        return dto;
    }

    /** Map Spoonacular Get Recipe Information response to RecipeDTO. */
    @SuppressWarnings("unchecked")
    private RecipeDTO mapSpoonacularDetailToDTO(Map<String, Object> info) {
        if (info == null) return null;
        RecipeDTO dto = new RecipeDTO();
        Object idObj = info.get("id");
        if (idObj instanceof Number) dto.setExternalRecipeId(((Number) idObj).longValue());
        dto.setRecipeName(info.get("title") != null ? info.get("title").toString() : "Unknown");
        dto.setImageUrl(info.get("image") != null ? info.get("image").toString() : "");
        int ready = info.get("readyInMinutes") instanceof Number ? ((Number) info.get("readyInMinutes")).intValue() : 30;
        dto.setPrepTimeMinutes(ready / 3);
        dto.setCookTimeMinutes(ready * 2 / 3);
        dto.setServings(info.get("servings") instanceof Number ? ((Number) info.get("servings")).intValue() : 4);
        dto.setMealType(SavedRecipe.MealType.ALL);
        String cat = "";
        if (Boolean.TRUE.equals(info.get("vegan"))) cat = "Vegan";
        else if (Boolean.TRUE.equals(info.get("vegetarian"))) cat = "Vegetarian";
        dto.setCategory(cat);

        List<Map<String, Object>> ingredients = new ArrayList<>();
        Object ingList = info.get("ingredients");
        if (ingList instanceof List) {
            for (Object o : (List<?>) ingList) {
                if (o instanceof Map) {
                    Map<String, Object> im = (Map<String, Object>) o;
                    Map<String, Object> mapped = new HashMap<>();
                    mapped.put("name", im.get("name"));
                    mapped.put("quantity", im.getOrDefault("amount", 1.0));
                    mapped.put("unit", im.getOrDefault("unit", ""));
                    ingredients.add(mapped);
                }
            }
        }
        dto.setIngredients(ingredients);
        String instr = info.get("instructions") != null ? info.get("instructions").toString().trim() : "";
        dto.setInstructions(!instr.isEmpty() ? instr : "Step-by-step instructions were not available for this recipe. You can still save it and look up cooking instructions online.");
        return dto;
    }

    /** Discover recipes by cuisine/diet (no pantry needed). For browsing. */
    public List<RecipeDTO> discoverRecipes(String query, String cuisine, String diet) {
        return discoverRecipes(query, cuisine, diet, null, 0);
    }

    /** Discover recipes with optional dietary rules (NO_GLUTEN, NO_DAIRY, VEGAN_ONLY). */
    @SuppressWarnings("unchecked")
    public List<RecipeDTO> discoverRecipes(String query, String cuisine, String diet, java.util.Map<String, Object> dietaryRules, int offset) {
        if (spoonacularClient == null || !spoonacularClient.isConfigured()) return new ArrayList<>();
        String dietParam = diet;
        StringBuilder intolerances = new StringBuilder();
        if (dietaryRules != null && !dietaryRules.isEmpty()) {
            if (Boolean.TRUE.equals(dietaryRules.get("VEGAN_ONLY"))) {
                dietParam = "vegan";
            }
            if (Boolean.TRUE.equals(dietaryRules.get("NO_GLUTEN"))) {
                if (intolerances.length() > 0) intolerances.append(",");
                intolerances.append("gluten");
            }
            if (Boolean.TRUE.equals(dietaryRules.get("NO_DAIRY"))) {
                if (intolerances.length() > 0) intolerances.append(",");
                intolerances.append("dairy");
            }
        }
        String intol = intolerances.length() > 0 ? intolerances.toString() : null;
        List<Map<String, Object>> results = spoonacularClient.discoverRecipes(query, cuisine, dietParam, intol, offset);
        List<RecipeDTO> dtos = new ArrayList<>();
        for (Map<String, Object> r : results) {
            RecipeDTO dto = mapSpoonacularSearchToDTO(r);
            if (dto != null) dtos.add(dto);
        }
        return dtos;
    }

    /** TheMealDB fallback when Spoonacular not configured. */
    private List<RecipeDTO> suggestRecipesTheMealDB(List<PantryItem> validPantryItems, RecipeSuggestionRequest request) {
        Map<String, List<String>> ingredientRecipeMap = searchRecipesByIngredientsParallel(validPantryItems);
        if (ingredientRecipeMap.isEmpty()) return new ArrayList<>();

        Map<String, Integer> recipeMatchCount = findRecipeIntersections(ingredientRecipeMap);
        if (recipeMatchCount.isEmpty()) return new ArrayList<>();

        List<String> sortedRecipeIds = recipeMatchCount.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .limit(50)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (request.getArea() != null && !request.getArea().trim().isEmpty()) {
            Set<String> areaIds = getRecipeIdsByArea(request.getArea().trim());
            if (!areaIds.isEmpty()) {
                sortedRecipeIds = sortedRecipeIds.stream().filter(areaIds::contains).limit(30).collect(Collectors.toList());
            }
        } else {
            sortedRecipeIds = sortedRecipeIds.stream().limit(30).collect(Collectors.toList());
        }

        List<RecipeDTO> suggestedRecipes = fetchRecipeDetailsParallel(sortedRecipeIds, validPantryItems, request);
        suggestedRecipes.sort((r1, r2) -> {
            boolean r1Expiring = r1.getUsesExpiringIngredients();
            boolean r2Expiring = r2.getUsesExpiringIngredients();
            if (r1Expiring != r2Expiring) return r2Expiring ? 1 : -1;
            return Double.compare(r2.getMatchPercentage(), r1.getMatchPercentage());
        });
        return suggestedRecipes;
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
                getThemealdbBaseUrlNormalized(),
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
        String diet = request.getDiet() != null ? request.getDiet().toUpperCase() : "ALL";
        Map<String, Object> dietaryRules = request.getDietaryRules();
        for (CompletableFuture<RecipeDTO> future : futures) {
            try {
                RecipeDTO recipe = future.get(5, TimeUnit.SECONDS);
                if (recipe != null) {
                    if (!passesDietFilter(recipe.getCategory(), diet)) {
                        continue;
                    }
                    if (dietaryRules != null && !dietaryRules.isEmpty() && !passesDietaryRules(recipe, dietaryRules)) {
                        continue;
                    }
                    if (request.getMealType() == SavedRecipe.MealType.ALL ||
                        recipe.getMealType() == request.getMealType()) {
                        recipes.add(recipe);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching recipe details in parallel: " + e.getMessage());
            }
        }

        return recipes;
    }

    /** Returns true if recipe passes user dietary rules (no gluten, no dairy, vegan only, etc.). */
    @SuppressWarnings("unchecked")
    private boolean passesDietaryRules(RecipeDTO recipe, Map<String, Object> rules) {
        if (rules == null) return true;
        List<String> ingredientNames = new ArrayList<>();
        if (recipe.getIngredients() != null) {
            for (Map<String, Object> ing : recipe.getIngredients()) {
                Object name = ing.get("name");
                if (name != null) ingredientNames.add(name.toString().toLowerCase());
            }
        }
        if (Boolean.TRUE.equals(rules.get("NO_GLUTEN"))) {
            if (ingredientNames.stream().anyMatch(n -> n.contains("wheat") || n.contains("flour") || n.contains("bread") || n.contains("pasta") || n.contains("gluten"))) {
                return false;
            }
        }
        if (Boolean.TRUE.equals(rules.get("NO_DAIRY"))) {
            if (ingredientNames.stream().anyMatch(n -> n.contains("milk") || n.contains("cheese") || n.contains("butter") || n.contains("cream") || n.contains("yogurt"))) {
                return false;
            }
        }
        if (Boolean.TRUE.equals(rules.get("VEGAN_ONLY"))) {
            String cat = (recipe.getCategory() != null) ? recipe.getCategory().toUpperCase() : "";
            if (!"VEGAN".equals(cat)) return false;
        }
        return true;
    }

    /** Returns true if recipe category matches the diet filter (ALL, VEGETARIAN, NON_VEGETARIAN). */
    private boolean passesDietFilter(String category, String diet) {
        if (diet == null || "ALL".equals(diet)) {
            return true;
        }
        String cat = (category != null) ? category.trim().toUpperCase() : "";
        boolean isVeg = "VEGETARIAN".equals(cat) || "VEGAN".equals(cat);
        if ("VEGETARIAN".equals(diet)) {
            return isVeg;
        }
        if ("NON_VEGETARIAN".equals(diet)) {
            return !isVeg; // exclude only Vegetarian/Vegan
        }
        return true;
    }

    /**
     * Gets meal IDs from TheMealDB by area/cuisine (e.g. Indian, American).
     */
    private Set<String> getRecipeIdsByArea(String area) {
        try {
            String url = String.format("%s/filter.php?a=%s",
                getThemealdbBaseUrlNormalized(),
                java.net.URLEncoder.encode(area, java.nio.charset.StandardCharsets.UTF_8));
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            Set<String> ids = new HashSet<>();
            JsonNode meals = jsonResponse.path("meals");
            if (meals != null && meals.isArray()) {
                for (JsonNode meal : meals) {
                    String id = meal.path("idMeal").asText();
                    if (id != null && !id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            System.err.println("Error fetching recipes by area " + area + ": " + e.getMessage());
            return Collections.emptySet();
        }
    }

    /** Spoonacular-supported cuisines (used when Spoonacular is configured). */
    private static final List<String> SPOONACULAR_CUISINES = Arrays.asList(
        "African", "Asian", "American", "British", "Cajun", "Caribbean", "Chinese", "Eastern European",
        "European", "French", "German", "Greek", "Indian", "Irish", "Italian", "Japanese", "Jewish",
        "Korean", "Latin American", "Mediterranean", "Mexican", "Middle Eastern", "Nordic", "Southern", "World");

    /**
     * Returns list of cuisine/area names. Uses Spoonacular cuisines when Spoonacular configured.
     */
    public List<String> getAreas() {
        if (spoonacularClient != null && spoonacularClient.isConfigured()) {
            return new ArrayList<>(SPOONACULAR_CUISINES);
        }
        try {
            String url = getThemealdbBaseUrlNormalized() + "/list.php?a=list";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            List<String> areas = new ArrayList<>();
            JsonNode meals = jsonResponse.path("meals");
            if (meals != null && meals.isArray()) {
                for (JsonNode m : meals) {
                    String a = m.path("strArea").asText("").trim();
                    if (!a.isEmpty()) areas.add(a);
                }
            }
            Collections.sort(areas);
            return areas;
        } catch (Exception e) {
            System.err.println("Error fetching areas: " + e.getMessage());
            return new ArrayList<>(SPOONACULAR_CUISINES);
        }
    }

    /**
     * Fetches detailed recipe information from TheMealDB API
     * Uses caching to avoid redundant API calls
     * 
     * @param recipeId TheMealDB recipe ID
     * @return RecipeDTO with full details, or null if not found
     */
    private RecipeDTO fetchRecipeDetails(String recipeId) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            System.err.println("Recipe ID is null or empty");
            return null;
        }
        
        // Check cache first
        if (recipeCache.containsKey(recipeId)) {
            Long timestamp = cacheTimestamps.get(recipeId);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
                return recipeCache.get(recipeId);
            }
        }

        try {
            String url = String.format("%s/lookup.php?i=%s", getThemealdbBaseUrlNormalized(), recipeId);
            System.out.println("Fetching recipe from TheMealDB: " + url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getBody() == null || response.getBody().trim().isEmpty()) {
                System.err.println("Empty response from TheMealDB API for recipe: " + recipeId);
                return null;
            }
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            JsonNode meals = jsonResponse.path("meals");
            if (meals == null || !meals.isArray() || meals.size() == 0) {
                System.err.println("No meals found in TheMealDB response for recipe: " + recipeId);
                return null;
            }

            JsonNode recipeJson = meals.get(0);
            if (recipeJson == null || recipeJson.isNull()) {
                System.err.println("Recipe JSON is null for recipe: " + recipeId);
                return null;
            }
            
            RecipeDTO recipeDTO = parseTheMealDBRecipe(recipeJson);

            // Cache the recipe
            if (recipeDTO != null) {
                recipeCache.put(recipeId, recipeDTO);
                cacheTimestamps.put(recipeId, System.currentTimeMillis());
            } else {
                System.err.println("Failed to parse recipe DTO for recipe: " + recipeId);
            }

            return recipeDTO;

        } catch (RestClientException e) {
            System.err.println("Error calling TheMealDB API for recipe: " + recipeId + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing TheMealDB recipe: " + recipeId + " - " + e.getMessage());
            e.printStackTrace();
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
            
            // Category (used to infer meal type and diet filter)
            String category = recipeJson.path("strCategory").asText("");
            recipeDTO.setCategory(category);
            String categoryUpper = category.toUpperCase();
            
            // TheMealDB doesn't provide prep/cook time, so we'll estimate based on category
            int estimatedTime = estimateCookingTime(categoryUpper);
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
            SavedRecipe.MealType mealType = determineMealType(categoryUpper);
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
        savedRecipe.setRecipeName(recipeDTO.getRecipeName() != null ? recipeDTO.getRecipeName().trim() : "");
        savedRecipe.setMealType(recipeDTO.getMealType() != null ? recipeDTO.getMealType() : SavedRecipe.MealType.ALL);
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

    /**
     * Gets enhanced recipe details with pantry matching and unit conversion
     * 
     * @param userId User's ID
     * @param recipeId External recipe ID (for suggested recipes) or saved recipe ID
     * @return Enhanced RecipeDTO with pantry matching information
     */
    public RecipeDTO getEnhancedRecipeDetails(Long userId, Long recipeId) {
        // Get user's pantry items
        List<PantryItem> pantryItems = pantryItemRepository.findByUserId(userId);
        
        // Try to fetch as saved recipe first
        RecipeDTO recipe = null;
        try {
            SavedRecipe savedRecipe = savedRecipeRepository.findById(recipeId)
                .orElse(null);
            if (savedRecipe != null && savedRecipe.getUser().getId().equals(userId)) {
                recipe = mapToDTO(savedRecipe);
            }
        } catch (Exception e) {
            System.err.println("Error fetching saved recipe: " + e.getMessage());
            // Not a saved recipe, continue to fetch from external API
        }
        
        // If not found as saved recipe, fetch from external API (Spoonacular or TheMealDB)
        if (recipe == null) {
            if (spoonacularClient != null && spoonacularClient.isConfigured()) {
                Map<String, Object> info = spoonacularClient.getRecipeInformation(recipeId);
                if (info != null) {
                    recipe = mapSpoonacularDetailToDTO(info);
                }
            }
            if (recipe == null && (spoonacularClient == null || !spoonacularClient.isConfigured())) {
                try {
                    recipe = fetchRecipeDetails(recipeId.toString());
                } catch (RestClientException e) {
                    System.err.println("Error calling recipe API for " + recipeId + ": " + e.getMessage());
                    throw new RuntimeException("Recipe service temporarily unavailable. Please try again later.", e);
                } catch (Exception e) {
                    System.err.println("Error fetching recipe: " + e.getMessage());
                    throw new RuntimeException("Failed to fetch recipe details: " + e.getMessage(), e);
                }
            }
            if (recipe == null) {
                throw new RuntimeException("Recipe not found with ID: " + recipeId);
            }
        }
        
        // Ensure ingredients list exists (initialize empty if null)
        if (recipe.getIngredients() == null) {
            recipe.setIngredients(new ArrayList<>());
        }
        
        // Enhance ingredients with pantry matching
        List<Map<String, Object>> enhancedIngredients = new ArrayList<>();
        List<Map<String, Object>> missingIngredients = new ArrayList<>();
        
        for (Map<String, Object> ingredient : recipe.getIngredients()) {
            String ingredientName = ingredient.get("name").toString();
            Double requiredQuantity = getDoubleValue(ingredient.get("quantity"));
            String requiredUnit = ingredient.get("unit") != null ? ingredient.get("unit").toString() : "";
            
            // Infer unit if missing
            if (requiredUnit == null || requiredUnit.isEmpty()) {
                requiredUnit = inferUnitFromIngredient(ingredientName);
            }
            
            // Normalize ingredient name for matching
            String normalizedIngredientName = normalizeIngredientName(ingredientName);
            
            // Find matching pantry item
            PantryItem matchingItem = findMatchingPantryItem(normalizedIngredientName, pantryItems);
            
            Map<String, Object> enhancedIngredient = new HashMap<>(ingredient);
            
            if (matchingItem != null) {
                // Item found in pantry
                String pantryUnit = matchingItem.getUnit();
                Double pantryQuantity = matchingItem.getQuantity();
                
                // Try to convert units if needed
                Double convertedRequiredQuantity = requiredQuantity;
                if (!requiredUnit.equals(pantryUnit)) {
                    Double converted = unitConversionService.convertQuantity(
                        requiredQuantity, requiredUnit, pantryUnit);
                    if (converted != null) {
                        convertedRequiredQuantity = converted;
                    }
                }
                
                // Determine status
                String status;
                if (convertedRequiredQuantity > pantryQuantity) {
                    status = "Insufficient";
                } else if (pantryQuantity <= matchingItem.getThreshold()) {
                    status = "Low Stock";
                } else {
                    status = "Available";
                }
                
                enhancedIngredient.put("pantryQuantity", pantryQuantity);
                enhancedIngredient.put("pantryUnit", pantryUnit);
                enhancedIngredient.put("status", status);
                enhancedIngredient.put("requiredQuantity", requiredQuantity);
                enhancedIngredient.put("requiredUnit", requiredUnit);
            } else {
                // Item not in pantry
                enhancedIngredient.put("status", "Not in Pantry");
                enhancedIngredient.put("requiredQuantity", requiredQuantity);
                enhancedIngredient.put("requiredUnit", requiredUnit);
                missingIngredients.add(enhancedIngredient);
            }
            
            enhancedIngredients.add(enhancedIngredient);
        }
        
        recipe.setIngredients(enhancedIngredients);
        recipe.setMissingIngredients(missingIngredients);
        
        // Format instructions
        if (recipe.getInstructions() != null) {
            recipe.setInstructions(formatInstructions(recipe.getInstructions()));
        }
        
        return recipe;
    }

    /**
     * Finds matching pantry item for an ingredient using fuzzy matching
     */
    private PantryItem findMatchingPantryItem(String normalizedIngredientName, List<PantryItem> pantryItems) {
        PantryItem bestMatch = null;
        double bestSimilarity = 0.0;
        double threshold = 0.7; // 70% similarity threshold
        
        for (PantryItem item : pantryItems) {
            String normalizedPantryName = normalizeIngredientName(item.getItemName());
            
            // Exact match
            if (normalizedPantryName.equals(normalizedIngredientName)) {
                return item;
            }
            
            // Fuzzy match using Levenshtein distance
            double similarity = calculateSimilarity(normalizedIngredientName, normalizedPantryName);
            if (similarity > bestSimilarity && similarity >= threshold) {
                bestSimilarity = similarity;
                bestMatch = item;
            }
        }
        
        return bestMatch;
    }

    /**
     * Calculates similarity between two strings (0.0 to 1.0)
     */
    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculates Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * Gets double value from object
     */
    private Double getDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Infers unit from ingredient name
     */
    private String inferUnitFromIngredient(String ingredientName) {
        String lower = ingredientName.toLowerCase();
        
        // Check for common patterns
        if (lower.contains("clove") || lower.contains("piece") || lower.contains("whole")) {
            return "pieces";
        }
        if (lower.contains("cup") || lower.contains("cups")) {
            return "cups";
        }
        if (lower.contains("tbsp") || lower.contains("tablespoon")) {
            return "tbsp";
        }
        if (lower.contains("tsp") || lower.contains("teaspoon")) {
            return "tsp";
        }
        
        // Default to pieces for countable items
        return "pieces";
    }

    /**
     * Formats recipe instructions by cleaning and re-numbering
     */
    private String formatInstructions(String instructions) {
        if (instructions == null || instructions.trim().isEmpty()) {
            return "";
        }
        
        // Split by common delimiters
        String[] steps = instructions.split("\\n+|\\r+|(?<=\\.)\\s+(?=\\d+\\.)|(?<=\\.)\\s+(?=[A-Z])");
        
        List<String> cleanedSteps = new ArrayList<>();
        int stepNumber = 1;
        
        for (String step : steps) {
            step = step.trim();
            if (step.isEmpty()) {
                continue;
            }
            
            // Remove existing numbering patterns (e.g., "1.", "2.", "Step 1:", etc.)
            step = step.replaceAll("^\\d+\\.\\s*", "")
                      .replaceAll("^Step\\s+\\d+:?\\s*", "")
                      .replaceAll("^\\d+\\)\\s*", "")
                      .trim();
            
            if (!step.isEmpty()) {
                cleanedSteps.add(stepNumber + ". " + step);
                stepNumber++;
            }
        }
        
        return String.join("\n\n", cleanedSteps);
    }

    /**
     * Marks pantry items as used when cooking a recipe
     * 
     * @param userId User's ID
     * @param recipeId Recipe ID
     * @param adjustedIngredients Map of ingredient names to adjusted quantities
     */
    public void cookRecipe(Long userId, Long recipeId, Map<String, Double> adjustedIngredients) {
        // Get enhanced recipe details
        RecipeDTO recipe = getEnhancedRecipeDetails(userId, recipeId);
        
        // Get user's pantry items
        List<PantryItem> pantryItems = pantryItemRepository.findByUserId(userId);
        
        // Process each ingredient
        for (Map<String, Object> ingredient : recipe.getIngredients()) {
            String ingredientName = ingredient.get("name").toString();
            String normalizedName = normalizeIngredientName(ingredientName);
            
            // Get adjusted quantity if provided
            Double quantityToUse = adjustedIngredients != null && adjustedIngredients.containsKey(ingredientName)
                ? adjustedIngredients.get(ingredientName)
                : getDoubleValue(ingredient.get("requiredQuantity"));
            
            // Find matching pantry item
            PantryItem matchingItem = findMatchingPantryItem(normalizedName, pantryItems);
            
            if (matchingItem != null && quantityToUse != null && quantityToUse > 0) {
                // Convert units if needed
                String requiredUnit = ingredient.get("requiredUnit") != null 
                    ? ingredient.get("requiredUnit").toString() 
                    : "pieces";
                String pantryUnit = matchingItem.getUnit();
                
                Double convertedQuantity = quantityToUse;
                if (!requiredUnit.equals(pantryUnit)) {
                    Double converted = unitConversionService.convertQuantity(
                        quantityToUse, requiredUnit, pantryUnit);
                    if (converted != null) {
                        convertedQuantity = converted;
                    }
                }
                
                // Update pantry item quantity
                Double newQuantity = matchingItem.getQuantity() - convertedQuantity;
                if (newQuantity <= 0) {
                    // Remove item if quantity becomes zero or negative
                    pantryItemRepository.delete(matchingItem);
                } else {
                    matchingItem.setQuantity(newQuantity);
                    pantryItemRepository.save(matchingItem);
                }
            }
        }
    }
}
