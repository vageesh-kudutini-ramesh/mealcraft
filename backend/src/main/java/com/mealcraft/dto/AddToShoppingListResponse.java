package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToShoppingListResponse {
    private int addedCount;
    private String weekStart; // yyyy-MM-dd for undo
    private boolean alreadyAdded; // true when items from week exist but no new slots to add
}
