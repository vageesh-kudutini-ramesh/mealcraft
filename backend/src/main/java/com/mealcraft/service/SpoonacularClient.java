package com.mealcraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Spoonacular API client with rate-limit-friendly usage.
 * Uses findByIngredients (real match %) when no filters, complexSearch when diet/cuisine/intolerances needed.
 * Get Recipe Information for details. All responses cached 6 hours.
 */
@Component
public class SpoonacularClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spoonacular.api.key:}")
    private String apiKey;

    @Value("${spoonacular.api.base-url:https://api.spoonacular.com}")
    private String baseUrl;

    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours (free tier optimization)
    private final Map<String, CachedResult> searchCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, CachedResult> recipeCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** Max ingredients to send (avoid long URLs, stay within API limits). */
    private static final int MAX_INGREDIENTS = 12;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !"YOUR_API_KEY_HERE".equals(apiKey.trim());
    }

    /**
     * Search recipes by ingredients. Uses findByIngredients when no filters (real match %),
     * complexSearch when diet/cuisine/intolerances are set.
     * 1 API call. Returns list of {id, title, image, usedIngredientCount, missedIngredientCount, matchPercentage, readyInMinutes, servings}.
     * @param offset Used for refresh - different offsets return different result sets (paginated).
     */
    public List<Map<String, Object>> searchByIngredients(List<String> ingredients, String diet, String intolerances, String cuisine, int offset) {
        if (!isConfigured() || ingredients == null || ingredients.isEmpty()) return Collections.emptyList();

        boolean hasFilters = hasFilters(diet, intolerances, cuisine);
        String cacheKey = "s:" + String.join(",", ingredients.subList(0, Math.min(ingredients.size(), MAX_INGREDIENTS)))
            + "|d:" + diet + "|i:" + intolerances + "|c:" + cuisine + "|o:" + offset;
        CachedResult cached = getIfValid(searchCache, cacheKey);
        if (cached != null) return (List<Map<String, Object>>) cached.data;

        List<String> limited = ingredients.size() > MAX_INGREDIENTS ? ingredients.subList(0, MAX_INGREDIENTS) : ingredients;
        List<Map<String, Object>> list;
        if (hasFilters) {
            list = searchComplexSearch(limited, diet, intolerances, cuisine, offset);
        } else {
            list = searchFindByIngredients(limited, offset);
        }
        if (!list.isEmpty()) {
            putCache(searchCache, cacheKey, list);
        }
        return list;
    }

    private boolean hasFilters(String diet, String intolerances, String cuisine) {
        return (diet != null && !diet.isEmpty() && !"ALL".equalsIgnoreCase(diet))
            || (intolerances != null && !intolerances.trim().isEmpty())
            || (cuisine != null && !cuisine.trim().isEmpty());
    }

    /** findByIngredients: real match %, 1 call, no diet/cuisine. Requests 30 for offset rotation. */
    private List<Map<String, Object>> searchFindByIngredients(List<String> ingredients, int offset) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(encode(ingredients.get(i)));
            }
            String url = getBaseUrl() + "/recipes/findByIngredients?ingredients=" + sb
                + "&number=30&ranking=1&ignorePantry=true&apiKey=" + apiKey.trim();
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (resp.getBody() == null) {
                System.err.println("[Spoonacular] findByIngredients: empty response body");
                return Collections.emptyList();
            }

            JsonNode arr = objectMapper.readTree(resp.getBody());
            List<Map<String, Object>> list = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode r : arr) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.path("id").asLong());
                    m.put("title", r.path("title").asText(""));
                    String img = r.path("image").asText("");
                    if (!img.startsWith("http")) img = "https://img.spoonacular.com/recipes/" + img;
                    m.put("image", img);
                    int used = r.path("usedIngredientCount").asInt(0);
                    int missed = r.path("missedIngredientCount").asInt(0);
                    m.put("usedIngredientCount", used);
                    m.put("missedIngredientCount", missed);
                    double total = used + missed;
                    m.put("matchPercentage", total > 0 ? (used / total) * 100.0 : 80.0);
                    m.put("readyInMinutes", 30);
                    m.put("servings", 4);
                    list.add(m);
                }
            }
            if (list.isEmpty()) {
                System.out.println("[Spoonacular] findByIngredients returned 0 recipes for: " + ingredients);
                return list;
            }
            int start = Math.max(0, offset % Math.max(1, list.size()));
            int take = Math.min(10, list.size());
            if (list.size() <= 10) return list;
            List<Map<String, Object>> rotated = new ArrayList<>();
            for (int i = 0; i < take; i++) {
                rotated.add(list.get((start + i) % list.size()));
            }
            return rotated;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("[Spoonacular] API error " + e.getStatusCode() + ": " + e.getMessage());
            if (e.getStatusCode().value() == 401) {
                System.err.println("[Spoonacular] Check your API key in application.properties (spoonacular.api.key)");
            } else if (e.getStatusCode().value() == 402) {
                System.err.println("[Spoonacular] Daily quota exceeded. Free tier: 150 requests/day.");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[Spoonacular] findByIngredients error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** complexSearch: supports diet, cuisine, intolerances. Match % approximated to 85. */
    private List<Map<String, Object>> searchComplexSearch(List<String> ingredients, String diet, String intolerances, String cuisine, int offset) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(encode(ingredients.get(i)));
            }
            StringBuilder url = new StringBuilder(getBaseUrl());
            url.append("/recipes/complexSearch?includeIngredients=").append(sb);
            url.append("&number=10&offset=").append(Math.min(900, Math.max(0, offset)));
            url.append("&sort=max-used-ingredients&instructionsRequired=true");
            if (diet != null && !diet.isEmpty() && !"ALL".equalsIgnoreCase(diet)) {
                String d = "VEGETARIAN".equalsIgnoreCase(diet) ? "vegetarian" : "VEGAN".equalsIgnoreCase(diet) ? "vegan" : null;
                if (d != null) url.append("&diet=").append(d);
            }
            if (intolerances != null && !intolerances.isEmpty()) {
                url.append("&intolerances=").append(encode(intolerances));
            }
            if (cuisine != null && !cuisine.trim().isEmpty()) {
                url.append("&cuisine=").append(encode(cuisine));
            }
            url.append("&apiKey=").append(apiKey.trim());

            ResponseEntity<String> resp = restTemplate.getForEntity(url.toString(), String.class);
            if (resp.getBody() == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode results = root.path("results");
            List<Map<String, Object>> list = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode r : results) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.path("id").asLong());
                    m.put("title", r.path("title").asText(""));
                    String img = r.path("image").asText("");
                    if (!img.startsWith("http")) img = "https://img.spoonacular.com/recipes/" + img;
                    m.put("image", img);
                    m.put("matchPercentage", 85.0);
                    m.put("readyInMinutes", 30);
                    m.put("servings", 4);
                    list.add(m);
                }
            }
            if (list.isEmpty()) {
                System.out.println("[Spoonacular] complexSearch returned 0 recipes for: " + ingredients);
            }
            return list;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("[Spoonacular] complexSearch API error " + e.getStatusCode() + ": " + e.getMessage());
            if (e.getStatusCode().value() == 401) {
                System.err.println("[Spoonacular] Check API key in application.properties. Use 'API Key' from console, not API Hash.");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[Spoonacular] complexSearch error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Discover recipes by cuisine/diet/intolerances (no pantry needed). For browsing. Cached 6 hours.
     * @param intolerances Comma-separated (e.g. "gluten,dairy") or null
     * @param offset Pagination offset (0–900) for refresh to return different results
     */
    public List<Map<String, Object>> discoverRecipes(String query, String cuisine, String diet, String intolerances, int offset) {
        if (!isConfigured()) return Collections.emptyList();
        String cacheKey = "d:" + (query != null ? query : "") + "|c:" + (cuisine != null ? cuisine : "") + "|d:" + (diet != null ? diet : "") + "|i:" + (intolerances != null ? intolerances : "") + "|o:" + offset;
        CachedResult cached = getIfValid(searchCache, cacheKey);
        if (cached != null) return (List<Map<String, Object>>) cached.data;

        try {
            int safeOffset = Math.min(900, Math.max(0, offset));
            StringBuilder url = new StringBuilder(getBaseUrl());
            url.append("/recipes/complexSearch?number=15&offset=").append(safeOffset).append("&instructionsRequired=true");
            if (query != null && !query.trim().isEmpty()) url.append("&query=").append(encode(query.trim()));
            if (cuisine != null && !cuisine.trim().isEmpty()) url.append("&cuisine=").append(encode(cuisine.trim()));
            if (diet != null && !diet.isEmpty() && !"ALL".equalsIgnoreCase(diet)) {
                String d = "VEGETARIAN".equalsIgnoreCase(diet) ? "vegetarian" : "VEGAN".equalsIgnoreCase(diet) ? "vegan" : "GLUTENFREE".equalsIgnoreCase(diet) ? "gluten free" : null;
                if (d != null) url.append("&diet=").append(encode(d.trim()));
            }
            if (intolerances != null && !intolerances.trim().isEmpty()) {
                url.append("&intolerances=").append(encode(intolerances.trim()));
            }
            url.append("&apiKey=").append(apiKey.trim());

            ResponseEntity<String> resp = restTemplate.getForEntity(url.toString(), String.class);
            if (resp.getBody() == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode results = root.path("results");
            List<Map<String, Object>> list = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode r : results) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.path("id").asLong());
                    m.put("title", r.path("title").asText(""));
                    String img = r.path("image").asText("");
                    if (!img.startsWith("http")) img = "https://img.spoonacular.com/recipes/" + img;
                    m.put("image", img);
                    m.put("matchPercentage", null);
                    m.put("readyInMinutes", r.has("readyInMinutes") ? r.path("readyInMinutes").asInt(30) : 30);
                    m.put("servings", r.has("servings") ? r.path("servings").asInt(4) : 4);
                    list.add(m);
                }
            }
            if (!list.isEmpty()) putCache(searchCache, cacheKey, list);
            return list;
        } catch (Exception e) {
            System.err.println("[Spoonacular] discoverRecipes error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get full recipe details. Cached for 6 hours.
     */
    public Map<String, Object> getRecipeInformation(long recipeId) {
        if (!isConfigured()) return null;

        String cacheKey = "r:" + recipeId;
        CachedResult cached = getIfValid(recipeCache, cacheKey);
        if (cached != null) return (Map<String, Object>) cached.data;

        try {
            String url = getBaseUrl() + "/recipes/" + recipeId + "/information?includeNutrition=false&apiKey=" + apiKey.trim();
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (resp.getBody() == null) return null;

            JsonNode root = objectMapper.readTree(resp.getBody());
            Map<String, Object> m = new HashMap<>();
            m.put("id", root.path("id").asLong());
            m.put("title", root.path("title").asText(""));
            String img = root.path("image").asText("");
            if (!img.isEmpty() && !img.startsWith("http")) img = "https://img.spoonacular.com/recipes/" + img;
            m.put("image", img);
            m.put("readyInMinutes", root.path("readyInMinutes").asInt(30));
            m.put("servings", root.path("servings").asInt(4));
            m.put("vegetarian", root.path("vegetarian").asBoolean(false));
            m.put("vegan", root.path("vegan").asBoolean(false));
            m.put("glutenFree", root.path("glutenFree").asBoolean(false));
            m.put("dairyFree", root.path("dairyFree").asBoolean(false));

            List<Map<String, Object>> ingredients = new ArrayList<>();
            JsonNode ext = root.path("extendedIngredients");
            if (ext != null && ext.isArray()) {
                for (JsonNode ing : ext) {
                    Map<String, Object> im = new HashMap<>();
                    im.put("name", ing.path("name").asText(""));
                    im.put("original", ing.path("original").asText(""));
                    im.put("amount", ing.path("amount").asDouble(1.0));
                    im.put("unit", ing.path("unit").asText(""));
                    ingredients.add(im);
                }
            }
            m.put("ingredients", ingredients);

            String instructions = extractInstructions(root);
            m.put("instructions", instructions);
            putCache(recipeCache, cacheKey, m);
            return m;
        } catch (Exception e) {
            System.err.println("[Spoonacular] getRecipeInformation error for " + recipeId + ": " + e.getMessage());
            return null;
        }
    }

    /** Extracts instructions from Spoonacular response - tries analyzedInstructions, then instructions, with proper formatting. */
    private String extractInstructions(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        JsonNode analyzed = root.path("analyzedInstructions");
        if (analyzed != null && analyzed.isArray()) {
            for (JsonNode section : analyzed) {
                JsonNode steps = section.path("steps");
                if (steps != null && steps.isArray()) {
                    for (JsonNode s : steps) {
                        int num = s.path("number").asInt(0);
                        String step = s.path("step").asText("").trim();
                        if (!step.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n\n");
                            sb.append(num > 0 ? num + ". " : "").append(step);
                        }
                    }
                }
            }
        }
        if (sb.length() == 0) {
            String raw = root.path("instructions").asText("");
            if (raw != null && !raw.trim().isEmpty()) {
                String cleaned = raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                if (!cleaned.isEmpty()) {
                    String[] parts = cleaned.split("(?<=[.!?])\\s+(?=[A-Z0-9])|\\n+");
                    int n = 1;
                    for (String p : parts) {
                        p = p.trim();
                        if (!p.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n\n");
                            sb.append(n++).append(". ").append(p);
                        }
                    }
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "Step-by-step instructions were not available for this recipe. You can still save it and look up cooking instructions online.";
    }

    private String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private String getBaseUrl() {
        String b = baseUrl != null ? baseUrl.trim() : "https://api.spoonacular.com";
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    private static class CachedResult {
        final Object data;
        final long timestamp;

        CachedResult(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private CachedResult getIfValid(Map<String, CachedResult> cache, String key) {
        CachedResult c = cache.get(key);
        if (c == null) return null;
        if (System.currentTimeMillis() - c.timestamp > CACHE_TTL_MS) {
            cache.remove(key);
            return null;
        }
        return c;
    }

    private void putCache(Map<String, CachedResult> cache, String key, Object data) {
        cache.put(key, new CachedResult(data));
    }
}
