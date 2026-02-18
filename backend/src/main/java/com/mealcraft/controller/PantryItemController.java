package com.mealcraft.controller;

import com.mealcraft.dto.PantryItemDTO;
import com.mealcraft.model.PantryItem;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.PantryItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Pantry Item Controller
 * 
 * Handles pantry inventory management endpoints.
 * Supports CRUD operations, expiration tracking, and low-stock alerts.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/pantry")
@CrossOrigin(origins = "*")
public class PantryItemController {

    @Autowired
    private PantryItemService pantryItemService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets all pantry items for current user
     * 
     * GET /api/pantry
     * 
     * @param authentication Spring Security authentication object
     * @return List of PantryItemDTO
     */
    @GetMapping
    public ResponseEntity<List<PantryItemDTO>> getAllPantryItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<PantryItemDTO> items = pantryItemService.getAllPantryItems(user.getId());
        return ResponseEntity.ok(items);
    }

    /**
     * Gets pantry items by category
     * 
     * GET /api/pantry/category/{category}
     * 
     * @param authentication Spring Security authentication object
     * @param category Pantry category
     * @return List of PantryItemDTO
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<PantryItemDTO>> getPantryItemsByCategory(
            Authentication authentication,
            @PathVariable PantryItem.PantryCategory category) {
        User user = getCurrentUser(authentication);
        List<PantryItemDTO> items = pantryItemService.getPantryItemsByCategory(user.getId(), category);
        return ResponseEntity.ok(items);
    }

    /**
     * Gets expiring items (within 7 days). Optional ?localDate=yyyy-MM-dd for user's timezone.
     * GET /api/pantry/expiring
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<PantryItemDTO>> getExpiringItems(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate) {
        User user = getCurrentUser(authentication);
        List<PantryItemDTO> items = pantryItemService.getExpiringItems(user.getId(), localDate);
        return ResponseEntity.ok(items);
    }

    /**
     * Gets expired items
     * 
     * GET /api/pantry/expired
     * 
     * @param authentication Spring Security authentication object
     * @return List of PantryItemDTO
     */
    @GetMapping("/expired")
    public ResponseEntity<List<PantryItemDTO>> getExpiredItems(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate) {
        User user = getCurrentUser(authentication);
        List<PantryItemDTO> items = pantryItemService.getExpiredItems(user.getId(), localDate);
        return ResponseEntity.ok(items);
    }

    /**
     * Gets low-stock items
     * 
     * GET /api/pantry/low-stock
     * 
     * @param authentication Spring Security authentication object
     * @return List of PantryItemDTO
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<PantryItemDTO>> getLowStockItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<PantryItemDTO> items = pantryItemService.getLowStockItems(user.getId());
        return ResponseEntity.ok(items);
    }

    /**
     * Creates a new pantry item
     * 
     * POST /api/pantry
     * 
     * @param authentication Spring Security authentication object
     * @param pantryItemDTO Pantry item data
     * @return Created PantryItemDTO
     */
    @PostMapping
    public ResponseEntity<PantryItemDTO> createPantryItem(
            Authentication authentication,
            @Valid @RequestBody PantryItemDTO pantryItemDTO) {
        User user = getCurrentUser(authentication);
        PantryItemDTO created = pantryItemService.createPantryItem(user.getId(), pantryItemDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Updates an existing pantry item
     * 
     * PUT /api/pantry/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Pantry item ID
     * @param pantryItemDTO Updated pantry item data
     * @return Updated PantryItemDTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<PantryItemDTO> updatePantryItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PantryItemDTO pantryItemDTO) {
        User user = getCurrentUser(authentication);
        PantryItemDTO updated = pantryItemService.updatePantryItem(user.getId(), id, pantryItemDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a pantry item
     * 
     * DELETE /api/pantry/{id}
     * 
     * @param authentication Spring Security authentication object
     * @param id Pantry item ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePantryItem(Authentication authentication, @PathVariable Long id) {
        User user = getCurrentUser(authentication);
        pantryItemService.deletePantryItem(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes all expired items
     * 
     * DELETE /api/pantry/expired
     * 
     * @param authentication Spring Security authentication object
     * @return Success response
     */
    @DeleteMapping("/expired")
    public ResponseEntity<?> deleteAllExpiredItems(Authentication authentication) {
        User user = getCurrentUser(authentication);
        pantryItemService.deleteAllExpiredItems(user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Marks pantry item as used
     * 
     * POST /api/pantry/{id}/use
     * 
     * @param authentication Spring Security authentication object
     * @param id Pantry item ID
     * @param quantityUsed Quantity used (optional, defaults to full quantity)
     * @return Success response
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<?> markItemAsUsed(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) Double quantityUsed) {
        User user = getCurrentUser(authentication);
        pantryItemService.markItemAsUsed(user.getId(), id, quantityUsed);
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




