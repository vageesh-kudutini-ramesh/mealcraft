package com.mealcraft.service;

import com.mealcraft.dto.MealPlanDTO;
import com.mealcraft.dto.ShoppingListItemDTO;
import com.mealcraft.model.ShoppingListItem;
import com.mealcraft.model.User;
import com.mealcraft.repository.ShoppingListItemRepository;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.MealPlanService;
import com.mealcraft.service.PantryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Auto-generates shopping list from weekly meal plan
     * 
     * @param userId User's ID
     * @param startDate Start date of the week
     * @param endDate End date of the week
     * @return List of generated ShoppingListItemDTO
     */
    public List<ShoppingListItemDTO> generateShoppingListFromMealPlan(Long userId, 
                                                                      LocalDate startDate, 
                                                                      LocalDate endDate) {
        // Get weekly meal plan
        List<MealPlanDTO> mealPlans = mealPlanService.getWeeklyMealPlan(userId, startDate, endDate);
        
        // Get current pantry inventory
        List<com.mealcraft.dto.PantryItemDTO> pantryItems = pantryItemService.getAllPantryItems(userId);
        
        // Aggregate ingredients from meal plans
        Map<String, Double> ingredientTotals = new HashMap<>();
        Map<String, String> ingredientUnits = new HashMap<>();
        
        for (MealPlanDTO mealPlan : mealPlans) {
            if (mealPlan.getIngredients() != null) {
                for (Map<String, Object> ingredient : mealPlan.getIngredients()) {
                    String name = ingredient.get("name").toString();
                    Double quantity = Double.parseDouble(ingredient.get("quantity").toString());
                    String unit = ingredient.get("unit").toString();
                    
                    String key = name.toLowerCase() + "|" + unit.toLowerCase();
                    ingredientTotals.put(key, ingredientTotals.getOrDefault(key, 0.0) + quantity);
                    ingredientUnits.put(key, unit);
                }
            }
        }
        
        // Calculate needed quantities (subtract pantry inventory)
        Map<String, Double> neededItems = new HashMap<>();
        for (Map.Entry<String, Double> entry : ingredientTotals.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String ingredientName = parts[0];
            String unit = parts[1];
            Double totalNeeded = entry.getValue();
            
            // Find matching pantry item
            Double available = pantryItems.stream()
                .filter(item -> item.getItemName().toLowerCase().equals(ingredientName) &&
                               item.getUnit().toLowerCase().equals(unit))
                .map(com.mealcraft.dto.PantryItemDTO::getQuantity)
                .reduce(0.0, Double::sum);
            
            Double needed = totalNeeded - available;
            if (needed > 0) {
                neededItems.put(ingredientName + "|" + unit, needed);
            }
        }
        
        // Create shopping list items
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        for (Map.Entry<String, Double> entry : neededItems.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String itemName = parts[0];
            String unit = parts[1];
            Double quantity = entry.getValue();
            
            ShoppingListItem item = new ShoppingListItem();
            item.setItemName(itemName);
            item.setQuantity(quantity);
            item.setUnit(unit);
            item.setIsPurchased(false);
            item.setUser(user);
            
            shoppingListItemRepository.save(item);
        }
        
        return getUnpurchasedItems(userId);
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
        item.setIsPurchased(shoppingListItemDTO.getIsPurchased());
        item.setSuggestedExpirationDate(shoppingListItemDTO.getSuggestedExpirationDate());
        item.setCategory(shoppingListItemDTO.getCategory());
        item.setUser(user);

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
     * Marks shopping list item as purchased
     * 
     * @param userId User's ID
     * @param itemId Shopping list item ID
     */
    public void markAsPurchased(Long userId, Long itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Shopping list item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Shopping list item does not belong to user");
        }

        item.markAsPurchased();
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
        return dto;
    }
}

