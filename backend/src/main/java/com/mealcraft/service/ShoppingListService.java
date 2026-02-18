package com.mealcraft.service;

import com.mealcraft.dto.AddToShoppingListResponse;
import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.dto.PantryItemDTO;
import com.mealcraft.dto.ShoppingListItemDTO;
import com.mealcraft.model.MealPlanShoppingSync;
import com.mealcraft.model.ShoppingListItem;
import com.mealcraft.model.User;
import com.mealcraft.repository.MealPlanShoppingSyncRepository;
import com.mealcraft.repository.ShoppingListItemRepository;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.MealPlanService;
import com.mealcraft.service.PantryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shopping List Service
 * 
 * Handles shopping list management operations.
 * Supports auto-generation from meal plans and manual item management.
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class ShoppingListService {

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MealPlanService mealPlanService;

    @Autowired
    private PantryItemService pantryItemService;

    @Autowired
    private IngredientMatchingService ingredientMatchingService;

    @Autowired
    private UnitConversionService unitConversionService;

    @Autowired
    private MealPlanShoppingSyncRepository mealPlanShoppingSyncRepository;

    /**
     * Gets all shopping list items for a user
     * 
     * @param userId User's ID
     * @return List of ShoppingListItemDTO
     */
    public List<ShoppingListItemDTO> getAllShoppingListItems(Long userId) {
        List<ShoppingListItem> items = shoppingListItemRepository.findByUserId(userId);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets unpurchased shopping list items
     * 
     * @param userId User's ID
     * @return List of unpurchased ShoppingListItemDTO
     */
    public List<ShoppingListItemDTO> getUnpurchasedItems(Long userId) {
        List<ShoppingListItem> items = shoppingListItemRepository.findUnpurchasedItems(userId);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Counts shopping list items from a specific week (for "already added" check).
     */
    public long countItemsFromWeek(Long userId, String weekStart) {
        return shoppingListItemRepository.countByUserIdAndSourceWeek(userId, weekStart);
    }

    /**
     * Auto-generates shopping list from weekly meal plan.
     * Uses optimized matching: fuzzy ingredient names, unit normalization, and cross-unit conversion.
     * Pantry items are matched by normalized name (handles "chicken"/"chicken breast", "g"/"grams")
     * and subtracted with proper unit conversion (e.g. 1 lb chicken satisfies 450g need).
     * 
     * @param forceFull If true, remove existing week items first then add (replace behavior for "add again")
     * @return Added count and weekStart for undo
     */
    public AddToShoppingListResponse generateShoppingListFromMealPlan(Long userId, 
                                                                      LocalDate startDate, 
                                                                      LocalDate endDate,
                                                                      LocalDate addedDate,
                                                                      boolean forceFull) {
        String weekStart = startDate.toString();
        Set<String> syncedSlots = new HashSet<>(mealPlanShoppingSyncRepository.findSlotKeysByUserIdAndWeek(userId, weekStart));

        if (forceFull) {
            removeItemsFromMealPlanWeek(userId, weekStart);
            mealPlanShoppingSyncRepository.deleteByUserIdAndWeek(userId, weekStart);
            syncedSlots.clear();
        }

        List<MealPlanDTO> mealPlans = mealPlanService.getWeeklyMealPlan(userId, startDate, endDate);
        List<PantryItemDTO> pantryItems = pantryItemService.getAllPantryItems(userId);
        
        // For incremental: only process meal plans whose (date|mealType) is not yet synced
        Map<String, AggregatedIngredient> aggregated = new HashMap<>();
        List<String> processedSlotKeys = new ArrayList<>();

        for (MealPlanDTO mealPlan : mealPlans) {
            if (mealPlan == null || mealPlan.getIngredients() == null) continue;
            String slotKey = mealPlan.getDate().toString() + "|" + mealPlan.getMealType().name();
            if (syncedSlots.contains(slotKey)) continue; // already added, skip
            processedSlotKeys.add(slotKey);

            for (Map<String, Object> ingredient : mealPlan.getIngredients()) {
                try {
                    if (ingredient == null) continue;
                    Object nameObj = ingredient.get("name");
                if (nameObj == null || nameObj.toString().trim().isEmpty()) continue;
                Object qtyObj = ingredient.get("quantity");
                if (qtyObj == null) qtyObj = ingredient.get("requiredQuantity");
                if (qtyObj == null) qtyObj = ingredient.get("amount");
                double quantity = 0;
                try {
                    quantity = qtyObj != null ? Double.parseDouble(qtyObj.toString()) : 0;
                } catch (NumberFormatException e) { continue; }
                if (quantity <= 0) continue;
                Object unitObj = ingredient.get("unit");
                if (unitObj == null) unitObj = ingredient.get("requiredUnit");
                String rawUnit = (unitObj != null && !unitObj.toString().trim().isEmpty()) ? unitObj.toString().trim() : "pieces";
                String name = nameObj.toString().trim();
                String normName = ingredientMatchingService.normalizeIngredientName(name);
                if (normName.isEmpty()) continue;
                double[] canonical = unitConversionService.convertToCanonical(quantity, rawUnit);
                if (canonical == null) continue;
                aggregated.computeIfAbsent(normName, k -> new AggregatedIngredient(name, (int) canonical[1]))
                    .addCanonical(canonical[0]);
                } catch (Exception ex) {
                    continue;
                }
            }
        }
        
        // Subtract pantry (fuzzy name + cross-unit conversion) and collect needed items
        Map<String, NeededItem> neededItems = new HashMap<>();
        for (Map.Entry<String, AggregatedIngredient> e : aggregated.entrySet()) {
            AggregatedIngredient agg = e.getValue();
            double availableInCanonical = getPantryAvailableInCanonical(pantryItems, agg.displayName, agg.canonicalType);
            double neededCanonical = agg.canonicalTotal - availableInCanonical;
            if (neededCanonical > 0.0001) {
                String displayUnit = unitConversionService.formatDisplayUnit(neededCanonical, agg.canonicalType);
                double displayQty = unitConversionService.convertFromCanonicalToDisplay(neededCanonical, agg.canonicalType);
                double rounded = roundQuantity(displayQty);
                neededItems.put(e.getKey(), new NeededItem(agg.displayName, displayUnit, rounded));
            }
        }
        
        // Create shopping list items
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        for (NeededItem ni : neededItems.values()) {
            ShoppingListItem item = new ShoppingListItem();
            item.setItemName(ni.displayName);
            item.setQuantity(ni.quantity);
            item.setUnit(ni.unit);
            item.setIsPurchased(false);
            item.setUser(user);
            item.setSourceType("MEAL_PLAN_WEEK");
            item.setSourceWeekStart(startDate.toString());
            if (addedDate != null) {
                item.setAddedAt(addedDate);
            }
            shoppingListItemRepository.save(item);
        }

        for (String slotKey : processedSlotKeys) {
            MealPlanShoppingSync sync = new MealPlanShoppingSync();
            sync.setUserId(userId);
            sync.setWeekStart(weekStart);
            sync.setSlotKey(slotKey);
            mealPlanShoppingSyncRepository.save(sync);
        }

        boolean alreadyAdded = processedSlotKeys.isEmpty() && !syncedSlots.isEmpty();
        return new AddToShoppingListResponse(neededItems.size(), weekStart, alreadyAdded);
    }
    
    private double getPantryAvailableInCanonical(List<PantryItemDTO> pantryItems, String ingredientName, int canonicalType) {
        if (pantryItems == null) return 0;
        double total = 0;
        for (PantryItemDTO item : pantryItems) {
            if (item == null || item.getItemName() == null || item.getUnit() == null || item.getQuantity() == null)
                continue;
            if (!ingredientMatchingService.namesMatch(ingredientName, item.getItemName())) continue;
            double[] canonical = unitConversionService.convertToCanonical(item.getQuantity(), item.getUnit());
            if (canonical != null && (int) canonical[1] == canonicalType) {
                total += canonical[0];
            }
        }
        return total;
    }

    private static double roundQuantity(double q) {
        if (q >= 100) return Math.round(q);
        if (q >= 10) return Math.round(q * 10) / 10.0;
        if (q >= 1) return Math.round(q * 100) / 100.0;
        if (q >= 0.1) return Math.round(q * 100) / 100.0;
        return Math.round(q * 1000) / 1000.0;
    }
    
    private static class AggregatedIngredient {
        final String displayName;
        final int canonicalType; // 1=weight(grams), 2=volume(ml), 3=count
        double canonicalTotal;
        AggregatedIngredient(String displayName, int canonicalType) {
            this.displayName = displayName;
            this.canonicalType = canonicalType;
            this.canonicalTotal = 0;
        }
        void addCanonical(double q) { canonicalTotal += q; }
    }
    
    private static class NeededItem {
        final String displayName;
        final String unit;
        final double quantity;
        NeededItem(String displayName, String unit, double quantity) {
            this.displayName = displayName;
            this.unit = unit;
            this.quantity = quantity;
        }
    }

    /**
     * Counts meal plan ingredients (for this week) that are needed but not on the shopping list.
     * Used for notification: "X items from your meal plan aren't on your shopping list"
     */
    public int countMealPlanIngredientsNotOnShoppingList(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MealPlanDTO> mealPlans = mealPlanService.getWeeklyMealPlan(userId, startDate, endDate);
        List<PantryItemDTO> pantryItems = pantryItemService.getAllPantryItems(userId);
        List<ShoppingListItem> existingList = shoppingListItemRepository.findByUserId(userId);

        Map<String, AggregatedIngredient> aggregated = new HashMap<>();
        for (MealPlanDTO mealPlan : mealPlans) {
            if (mealPlan.getIngredients() == null) continue;
            for (Map<String, Object> ingredient : mealPlan.getIngredients()) {
                Object nameObj = ingredient.get("name");
                if (nameObj == null || nameObj.toString().trim().isEmpty()) continue;
                Object qtyObj = ingredient.get("quantity");
                if (qtyObj == null) qtyObj = ingredient.get("requiredQuantity");
                if (qtyObj == null) qtyObj = ingredient.get("amount");
                double quantity = 0;
                try {
                    quantity = qtyObj != null ? Double.parseDouble(qtyObj.toString()) : 0;
                } catch (NumberFormatException e) { continue; }
                if (quantity <= 0) continue;
                Object unitObj = ingredient.get("unit");
                if (unitObj == null) unitObj = ingredient.get("requiredUnit");
                String rawUnit = (unitObj != null && !unitObj.toString().trim().isEmpty()) ? unitObj.toString().trim() : "pieces";
                String name = nameObj.toString().trim();
                String normName = ingredientMatchingService.normalizeIngredientName(name);
                if (normName.isEmpty()) continue;
                double[] canonical = unitConversionService.convertToCanonical(quantity, rawUnit);
                if (canonical == null) continue;
                aggregated.computeIfAbsent(normName, k -> new AggregatedIngredient(name, (int) canonical[1]))
                    .addCanonical(canonical[0]);
            }
        }

        Map<String, NeededItem> neededItems = new HashMap<>();
        for (Map.Entry<String, AggregatedIngredient> e : aggregated.entrySet()) {
            AggregatedIngredient agg = e.getValue();
            double availableInCanonical = getPantryAvailableInCanonical(pantryItems, agg.displayName, agg.canonicalType);
            double neededCanonical = agg.canonicalTotal - availableInCanonical;
            if (neededCanonical > 0.0001) {
                String displayUnit = unitConversionService.formatDisplayUnit(neededCanonical, agg.canonicalType);
                double displayQty = unitConversionService.convertFromCanonicalToDisplay(neededCanonical, agg.canonicalType);
                double rounded = roundQuantity(displayQty);
                neededItems.put(e.getKey(), new NeededItem(agg.displayName, displayUnit, rounded));
            }
        }

        int notOnList = 0;
        for (NeededItem ni : neededItems.values()) {
            boolean found = existingList.stream().anyMatch(s -> 
                ingredientMatchingService.namesMatch(ni.displayName, s.getItemName()));
            if (!found) notOnList++;
        }
        return notOnList;
    }

    /**
     * Undo: removes all shopping list items that were added from a specific week's meal plan.
     *
     * @param userId User's ID
     * @param weekStart Week start date (yyyy-MM-dd)
     * @return Number of items removed
     */
    public int removeItemsFromMealPlanWeek(Long userId, String weekStart) {
        List<ShoppingListItem> items = shoppingListItemRepository.findByUserIdAndSourceWeek(userId, weekStart);
        int count = items.size();
        shoppingListItemRepository.deleteByUserIdAndSourceWeek(userId, weekStart);
        return count;
    }

    /**
     * Creates a shopping list item manually
     * 
     * @param userId User's ID
     * @param shoppingListItemDTO Shopping list item data
     * @return Created ShoppingListItemDTO
     */
    public ShoppingListItemDTO createShoppingListItem(Long userId, ShoppingListItemDTO shoppingListItemDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ShoppingListItem item = new ShoppingListItem();
        item.setItemName(shoppingListItemDTO.getItemName());
        item.setQuantity(shoppingListItemDTO.getQuantity());
        item.setUnit(shoppingListItemDTO.getUnit());
        item.setIsPurchased(shoppingListItemDTO.getIsPurchased() != null ? shoppingListItemDTO.getIsPurchased() : false);
        item.setSuggestedExpirationDate(shoppingListItemDTO.getSuggestedExpirationDate());
        item.setCategory(shoppingListItemDTO.getCategory());
        item.setUser(user);
        if (shoppingListItemDTO.getAddedAt() != null) {
            item.setAddedAt(shoppingListItemDTO.getAddedAt());
        }

        item = shoppingListItemRepository.save(item);
        return mapToDTO(item);
    }

    /**
     * Updates a shopping list item
     * 
     * @param userId User's ID
     * @param itemId Shopping list item ID
     * @param shoppingListItemDTO Updated shopping list item data
     * @return Updated ShoppingListItemDTO
     */
    public ShoppingListItemDTO updateShoppingListItem(Long userId, Long itemId, ShoppingListItemDTO shoppingListItemDTO) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Shopping list item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Shopping list item does not belong to user");
        }

        item.setItemName(shoppingListItemDTO.getItemName());
        item.setQuantity(shoppingListItemDTO.getQuantity());
        item.setUnit(shoppingListItemDTO.getUnit());
        item.setIsPurchased(shoppingListItemDTO.getIsPurchased());
        item.setSuggestedExpirationDate(shoppingListItemDTO.getSuggestedExpirationDate());
        item.setCategory(shoppingListItemDTO.getCategory());

        item = shoppingListItemRepository.save(item);
        return mapToDTO(item);
    }

    /**
     * Marks shopping list item as purchased (uses user's local date from frontend when provided).
     * 
     * @param userId User's ID
     * @param itemId Shopping list item ID
     * @param purchaseDate User's local date when they clicked purchase (null = use server date)
     */
    public void markAsPurchased(Long userId, Long itemId, LocalDate purchaseDate) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Shopping list item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Shopping list item does not belong to user");
        }

        item.markAsPurchased(purchaseDate);
        shoppingListItemRepository.save(item);
    }

    /**
     * Deletes a shopping list item
     * 
     * @param userId User's ID
     * @param itemId Shopping list item ID
     */
    public void deleteShoppingListItem(Long userId, Long itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Shopping list item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Shopping list item does not belong to user");
        }

        shoppingListItemRepository.delete(item);
    }

    /**
     * Clears all purchased items
     * 
     * @param userId User's ID
     */
    public void clearPurchasedItems(Long userId) {
        shoppingListItemRepository.deletePurchasedItems(userId);
    }

    /**
     * Clears all unpurchased (to-buy) items
     * 
     * @param userId User's ID
     */
    public void clearUnpurchasedItems(Long userId) {
        shoppingListItemRepository.deleteUnpurchasedItems(userId);
    }

    /**
     * Maps ShoppingListItem entity to ShoppingListItemDTO
     * 
     * @param item ShoppingListItem entity
     * @return ShoppingListItemDTO
     */
    private ShoppingListItemDTO mapToDTO(ShoppingListItem item) {
        ShoppingListItemDTO dto = new ShoppingListItemDTO();
        dto.setId(item.getId());
        dto.setItemName(item.getItemName());
        dto.setQuantity(item.getQuantity());
        dto.setUnit(item.getUnit());
        dto.setIsPurchased(item.getIsPurchased());
        dto.setSuggestedExpirationDate(item.getSuggestedExpirationDate());
        dto.setCategory(item.getCategory());
        dto.setSourceType(item.getSourceType());
        dto.setSourceWeekStart(item.getSourceWeekStart());
        dto.setAddedAt(item.getAddedAt() != null ? item.getAddedAt() : (item.getCreatedAt() != null ? item.getCreatedAt().toLocalDate() : null));
        dto.setPurchasedAt(item.getPurchasedAt());
        return dto;
    }
}

