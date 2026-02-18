package com.mealcraft.service;

import com.mealcraft.dto.PantryItemDTO;
import com.mealcraft.model.PantryItem;
import com.mealcraft.model.User;
import com.mealcraft.repository.PantryItemRepository;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pantry Item Service
 * 
 * Handles pantry inventory management operations including:
 * - Adding, updating, and deleting pantry items
 * - Tracking expiration dates and status
 * - Low-stock alerts
 * - Finding expiring and expired items
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class PantryItemService {

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets all pantry items for a user
     * 
     * @param userId User's ID
     * @return List of PantryItemDTO
     */
    public List<PantryItemDTO> getAllPantryItems(Long userId) {
        List<PantryItem> items = pantryItemRepository.findByUserId(userId);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets pantry items by category for a user
     * 
     * @param userId User's ID
     * @param category Pantry category
     * @return List of PantryItemDTO
     */
    public List<PantryItemDTO> getPantryItemsByCategory(Long userId, PantryItem.PantryCategory category) {
        List<PantryItem> items = pantryItemRepository.findByUserIdAndCategory(userId, category);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /** Number of days considered "expiring soon" – aligned with Pantry tab and notifications */
    private static final int EXPIRING_SOON_DAYS = 7;

    /**
     * Gets expiring items (within 7 days – today through 7 days from now).
     * Uses user's local date when provided (avoids timezone mismatch).
     * 
     * @param userId User's ID
     * @param localDate User's local date (null = use server date)
     * @return List of PantryItemDTO
     */
    public List<PantryItemDTO> getExpiringItems(Long userId, LocalDate localDate) {
        LocalDate today = localDate != null ? localDate : LocalDate.now();
        LocalDate endDate = today.plusDays(EXPIRING_SOON_DAYS);
        List<PantryItem> items = pantryItemRepository.findExpiringItems(userId, today, endDate);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets expired items. Uses user's local date when provided.
     * 
     * @param userId User's ID
     * @param localDate User's local date (null = use server date)
     * @return List of PantryItemDTO
     */
    public List<PantryItemDTO> getExpiredItems(Long userId, LocalDate localDate) {
        LocalDate today = localDate != null ? localDate : LocalDate.now();
        List<PantryItem> items = pantryItemRepository.findExpiredItems(userId, today);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets low-stock items
     * 
     * @param userId User's ID
     * @return List of PantryItemDTO
     */
    public List<PantryItemDTO> getLowStockItems(Long userId) {
        List<PantryItem> items = pantryItemRepository.findLowStockItems(userId);
        return items.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Creates a new pantry item
     * 
     * @param userId User's ID
     * @param pantryItemDTO Pantry item data
     * @return Created PantryItemDTO
     */
    public PantryItemDTO createPantryItem(Long userId, PantryItemDTO pantryItemDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        PantryItem item = new PantryItem();
        item.setItemName(pantryItemDTO.getItemName());
        item.setQuantity(pantryItemDTO.getQuantity());
        item.setUnit(pantryItemDTO.getUnit());
        item.setCategory(pantryItemDTO.getCategory());
        item.setExpirationDate(pantryItemDTO.getExpirationDate());
        item.setThreshold(pantryItemDTO.getThreshold());
        item.setUser(user);

        item = pantryItemRepository.save(item);
        return mapToDTO(item);
    }

    /**
     * Updates an existing pantry item
     * 
     * @param userId User's ID
     * @param itemId Pantry item ID
     * @param pantryItemDTO Updated pantry item data
     * @return Updated PantryItemDTO
     */
    public PantryItemDTO updatePantryItem(Long userId, Long itemId, PantryItemDTO pantryItemDTO) {
        PantryItem item = pantryItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Pantry item not found"));

        // Verify ownership
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Pantry item does not belong to user");
        }

        // Update fields
        item.setItemName(pantryItemDTO.getItemName());
        item.setQuantity(pantryItemDTO.getQuantity());
        item.setUnit(pantryItemDTO.getUnit());
        item.setCategory(pantryItemDTO.getCategory());
        item.setExpirationDate(pantryItemDTO.getExpirationDate());
        item.setThreshold(pantryItemDTO.getThreshold());

        item = pantryItemRepository.save(item);
        return mapToDTO(item);
    }

    /**
     * Deletes a pantry item
     * 
     * @param userId User's ID
     * @param itemId Pantry item ID
     */
    public void deletePantryItem(Long userId, Long itemId) {
        PantryItem item = pantryItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Pantry item not found"));

        // Verify ownership
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Pantry item does not belong to user");
        }

        pantryItemRepository.delete(item);
    }

    /**
     * Deletes all expired items for a user
     * 
     * @param userId User's ID
     */
    public void deleteAllExpiredItems(Long userId) {
        LocalDate today = LocalDate.now();
        pantryItemRepository.deleteExpiredItems(userId, today);
    }

    /**
     * Marks pantry item as used (decreases quantity or removes item)
     * 
     * @param userId User's ID
     * @param itemId Pantry item ID
     * @param quantityUsed Quantity used (optional, defaults to full quantity)
     */
    public void markItemAsUsed(Long userId, Long itemId, Double quantityUsed) {
        PantryItem item = pantryItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Pantry item not found"));

        // Verify ownership
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Pantry item does not belong to user");
        }

        if (quantityUsed == null || quantityUsed >= item.getQuantity()) {
            // Remove item if full quantity used or no quantity specified
            pantryItemRepository.delete(item);
        } else {
            // Decrease quantity
            item.setQuantity(item.getQuantity() - quantityUsed);
            pantryItemRepository.save(item);
        }
    }

    /**
     * Maps PantryItem entity to PantryItemDTO
     * 
     * @param item PantryItem entity
     * @return PantryItemDTO
     */
    private PantryItemDTO mapToDTO(PantryItem item) {
        PantryItemDTO dto = new PantryItemDTO();
        dto.setId(item.getId());
        dto.setItemName(item.getItemName());
        dto.setQuantity(item.getQuantity());
        dto.setUnit(item.getUnit());
        dto.setCategory(item.getCategory());
        dto.setExpirationDate(item.getExpirationDate());
        dto.setThreshold(item.getThreshold());
        dto.setDaysUntilExpiry(item.getDaysUntilExpiry());
        dto.setExpirationStatus(item.getExpirationStatus());
        dto.setIsLowStock(item.isLowStock());
        return dto;
    }
}




