package com.mealcraft.controller;

import com.mealcraft.dto.ShoppingListItemDTO;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.ShoppingListService;
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
 * Shopping List Controller
 * 
 * Handles shopping list management endpoints.
 * Supports auto-generation from meal plans and manual item management.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/shopping-list")
@CrossOrigin(origins = "*")
public class ShoppingListController {

    @Autowired
    private ShoppingListService shoppingListService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets all shopping list items for current user
     * 
     * GET /api/shopping-list
     * 
     * @param authentication Spring Security authentication object
     * @return List of ShoppingListItemDTO
     */
    @GetMapping
    public ResponseEntity<List<ShoppingListItemDTO>> getAllShoppingListItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<ShoppingListItemDTO> items = shoppingListService.getAllShoppingListItems(user.getId());
        return ResponseEntity.ok(items);
    }

    /**
     * Gets unpurchased shopping list items
     * 
     * GET /api/shopping-list/unpurchased
     * 
     * @param authentication Spring Security authentication object
     * @return List of unpurchased ShoppingListItemDTO
     */
    @GetMapping("/unpurchased")
    public ResponseEntity<List<ShoppingListItemDTO>> getUnpurchasedItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<ShoppingListItemDTO> items = shoppingListService.getUnpurchasedItems(user.getId());
        return ResponseEntity.ok(items);
    }

    /**
     * Auto-generates shopping list from weekly meal plan
     * 
     * POST /api/shopping-list/generate?startDate={startDate}&endDate={endDate}
     * 
     * @param authentication Spring Security authentication object
     * @param startDate Start date of the week
     * @param endDate End date of the week
     * @return List of generated ShoppingListItemDTO
     */
    @PostMapping("/generate")
    public ResponseEntity<List<ShoppingListItemDTO>> generateShoppingList(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User user = getCurrentUser(authentication);
        List<ShoppingListItemDTO> items = shoppingListService.generateShoppingListFromMealPlan(
            user.getId(), startDate, endDate);
        return ResponseEntity.ok(items);
    }

    /**
     * Creates a shopping list item manually
     * 
     * POST /api/shopping-list
     * 
     * @param authentication Spring Security authentication object
     * @param shoppingListItemDTO Shopping list item data
     * @return Created ShoppingListItemDTO
     */
    @PostMapping
    public ResponseEntity<ShoppingListItemDTO> createShoppingListItem(
            Authentication authentication,
            @Valid @RequestBody ShoppingListItemDTO shoppingListItemDTO) {
        User user = getCurrentUser(authentication);
        ShoppingListItemDTO created = shoppingListService.createShoppingListItem(user.getId(), shoppingListItemDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Updates a shopping list item
     * 
     * PUT /api/shopping-list/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Shopping list item ID
     * @param shoppingListItemDTO Updated shopping list item data
     * @return Updated ShoppingListItemDTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListItemDTO> updateShoppingListItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ShoppingListItemDTO shoppingListItemDTO) {
        User user = getCurrentUser(authentication);
        ShoppingListItemDTO updated = shoppingListService.updateShoppingListItem(user.getId(), id, shoppingListItemDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Marks shopping list item as purchased
     * 
     * POST /api/shopping-list/{id}/purchase
     * 
     * @param authentication Spring Security authentication object
     * @param id Shopping list item ID
     * @return Success response
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<?> markAsPurchased(Authentication authentication, @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        shoppingListService.markAsPurchased(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a shopping list item
     * 
     * DELETE /api/shopping-list/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Shopping list item ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShoppingListItem(Authentication authentication, @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        shoppingListService.deleteShoppingListItem(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Clears all purchased items
     * 
     * DELETE /api/shopping-list/purchased
     * 
     * @param authentication Spring Security authentication object
     * @return Success response
     */
    @DeleteMapping("/purchased")
    public ResponseEntity<?> clearPurchasedItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        shoppingListService.clearPurchasedItems(user.getId());
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




