package com.mealcraft.controller;

import com.mealcraft.dto.AddToShoppingListResponse;
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
import java.util.Map;

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
     * Check if items from this week are already on the shopping list.
     * GET /api/shopping-list/from-week-status?weekStart=yyyy-MM-dd
     */
    @GetMapping("/from-week-status")
    public ResponseEntity<?> getFromWeekStatus(
            Authentication authentication,
            @RequestParam String weekStart) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Authentication required"));
        }
        User user = getCurrentUser(authentication);
        long count = shoppingListService.countItemsFromWeek(user.getId(), weekStart);
        return ResponseEntity.ok(java.util.Map.of(
            "hasItemsFromWeek", count > 0,
            "count", count
        ));
    }

    /**
     * One-click: add this week's meal plan ingredients to shopping list (deduped, minus pantry).
     * Returns addedCount and weekStart so frontend can offer "Undo".
     * forceFull=true: replace existing week items (undo first, then add) - for "add again" flow.
     */
    @PostMapping("/from-week")
    public ResponseEntity<?> addFromMealPlanWeek(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate addedDate,
            @RequestParam(required = false, defaultValue = "false") boolean forceFull) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Authentication required"));
        }
        try {
            User user = getCurrentUser(authentication);
            AddToShoppingListResponse result = shoppingListService.generateShoppingListFromMealPlan(
                user.getId(), startDate, endDate, addedDate, forceFull);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Failed to add ingredients",
                "message", e.getMessage() != null ? e.getMessage() : "Internal server error"
            ));
        }
    }

    /**
     * Undo: remove all items that were added from a specific week's meal plan.
     */
    @PostMapping("/undo-from-week")
    public ResponseEntity<?> undoFromMealPlanWeek(
            Authentication authentication,
            @RequestParam String weekStart) {
        User user = getCurrentUser(authentication);
        int removed = shoppingListService.removeItemsFromMealPlanWeek(user.getId(), weekStart);
        return ResponseEntity.ok(java.util.Map.of("removedCount", removed));
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
     * Accepts optional body: {"purchasedDate": "yyyy-MM-dd"} for user's local date/timezone
     * 
     * POST /api/shopping-list/{id}/purchase
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<?> markAsPurchased(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = getCurrentUser(authentication);
        java.time.LocalDate purchaseDate = null;
        if (body != null && body.containsKey("purchasedDate")) {
            try {
                purchaseDate = java.time.LocalDate.parse(body.get("purchasedDate"));
            } catch (Exception ignored) { /* use server date */ }
        }
        shoppingListService.markAsPurchased(user.getId(), id, purchaseDate);
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
     * DELETE /api/shopping-list/purchased
     */
    @DeleteMapping("/purchased")
    public ResponseEntity<?> clearPurchasedItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        shoppingListService.clearPurchasedItems(user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Clears all unpurchased (to-buy) items
     * DELETE /api/shopping-list/unpurchased
     */
    @DeleteMapping("/unpurchased")
    public ResponseEntity<?> clearUnpurchasedItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        shoppingListService.clearUnpurchasedItems(user.getId());
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




