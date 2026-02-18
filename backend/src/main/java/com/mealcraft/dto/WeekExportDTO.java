package com.mealcraft.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for exporting a week's meal plan (share / PDF).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeekExportDTO {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<MealPlanDTO> plans;
    /** Optional: aggregated shopping list for this week (name, quantity, unit). */
    private List<Map<String, Object>> shoppingSummary;
}
