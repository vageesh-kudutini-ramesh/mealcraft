package com.mealcraft.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit Conversion Service
 * 
 * Handles conversions between various measurement units (weight, volume, count).
 * Supports unit aliases and conversions between compatible unit types.
 * 
 * @author MealCraft Team
 */
@Service
public class UnitConversionService {

    // Unit aliases mapping
    private static final Map<String, String> UNIT_ALIASES = new HashMap<>();
    
    // Conversion factors to base units
    private static final Map<String, Double> WEIGHT_CONVERSIONS = new HashMap<>();
    private static final Map<String, Double> VOLUME_CONVERSIONS = new HashMap<>();
    
    static {
        // Weight unit aliases
        UNIT_ALIASES.put("g", "grams");
        UNIT_ALIASES.put("gram", "grams");
        UNIT_ALIASES.put("kg", "kg");
        UNIT_ALIASES.put("kilogram", "kg");
        UNIT_ALIASES.put("kilograms", "kg");
        UNIT_ALIASES.put("lb", "lbs");
        UNIT_ALIASES.put("pound", "lbs");
        UNIT_ALIASES.put("pounds", "lbs");
        UNIT_ALIASES.put("oz", "oz");
        UNIT_ALIASES.put("ounce", "oz");
        UNIT_ALIASES.put("ounces", "oz");
        
        // Volume unit aliases
        UNIT_ALIASES.put("ml", "ml");
        UNIT_ALIASES.put("milliliter", "ml");
        UNIT_ALIASES.put("milliliters", "ml");
        UNIT_ALIASES.put("l", "liters");
        UNIT_ALIASES.put("liter", "liters");
        UNIT_ALIASES.put("litre", "liters");
        UNIT_ALIASES.put("cup", "cups");
        UNIT_ALIASES.put("tbsp", "tbsp");
        UNIT_ALIASES.put("tablespoon", "tbsp");
        UNIT_ALIASES.put("tablespoons", "tbsp");
        UNIT_ALIASES.put("tsp", "tsp");
        UNIT_ALIASES.put("teaspoon", "tsp");
        UNIT_ALIASES.put("teaspoons", "tsp");
        
        // Count unit aliases
        UNIT_ALIASES.put("piece", "pieces");
        UNIT_ALIASES.put("pcs", "pieces");
        UNIT_ALIASES.put("pc", "pieces");
        UNIT_ALIASES.put("item", "pieces");
        UNIT_ALIASES.put("items", "pieces");
        
        // Weight conversions (to grams)
        WEIGHT_CONVERSIONS.put("grams", 1.0);
        WEIGHT_CONVERSIONS.put("g", 1.0);
        WEIGHT_CONVERSIONS.put("kg", 1000.0);
        WEIGHT_CONVERSIONS.put("lbs", 453.592);
        WEIGHT_CONVERSIONS.put("oz", 28.3495);
        
        // Volume conversions (to ml)
        VOLUME_CONVERSIONS.put("ml", 1.0);
        VOLUME_CONVERSIONS.put("liters", 1000.0);
        VOLUME_CONVERSIONS.put("cups", 236.588);
        VOLUME_CONVERSIONS.put("tbsp", 14.7868);
        VOLUME_CONVERSIONS.put("tsp", 4.92892);
        VOLUME_CONVERSIONS.put("oz", 29.5735); // fluid ounce
    }

    /**
     * Normalizes a unit name using aliases
     * 
     * @param unit Unit name to normalize
     * @return Normalized unit name
     */
    public String normalizeUnit(String unit) {
        if (unit == null) {
            return "pieces";
        }
        String normalized = unit.toLowerCase().trim();
        return UNIT_ALIASES.getOrDefault(normalized, normalized);
    }

    /**
     * Converts quantity from one unit to another
     * Only converts between compatible unit types (weight to weight, volume to volume)
     * 
     * @param quantity Quantity to convert
     * @param fromUnit Source unit
     * @param toUnit Target unit
     * @return Converted quantity, or null if conversion is not possible
     */
    public Double convertQuantity(Double quantity, String fromUnit, String toUnit) {
        if (quantity == null || fromUnit == null || toUnit == null) {
            return null;
        }
        
        String normalizedFrom = normalizeUnit(fromUnit);
        String normalizedTo = normalizeUnit(toUnit);
        
        // If units are the same, no conversion needed
        if (normalizedFrom.equals(normalizedTo)) {
            return quantity;
        }
        
        // Check if both are weight units
        if (isWeightUnit(normalizedFrom) && isWeightUnit(normalizedTo)) {
            return convertWeight(quantity, normalizedFrom, normalizedTo);
        }
        
        // Check if both are volume units
        if (isVolumeUnit(normalizedFrom) && isVolumeUnit(normalizedTo)) {
            return convertVolume(quantity, normalizedFrom, normalizedTo);
        }
        
        // Check if both are count units
        if (isCountUnit(normalizedFrom) && isCountUnit(normalizedTo)) {
            return quantity; // Count units are 1:1
        }
        
        // Conversion not possible between different unit types
        return null;
    }

    /**
     * Checks if a unit is a weight unit
     */
    private boolean isWeightUnit(String unit) {
        return WEIGHT_CONVERSIONS.containsKey(unit);
    }

    /**
     * Checks if a unit is a volume unit
     */
    private boolean isVolumeUnit(String unit) {
        return VOLUME_CONVERSIONS.containsKey(unit);
    }

    /**
     * Checks if a unit is a count unit
     */
    private boolean isCountUnit(String unit) {
        return unit.equals("pieces") || unit.equals("piece");
    }

    /**
     * Converts weight from one unit to another
     */
    private Double convertWeight(Double quantity, String fromUnit, String toUnit) {
        Double fromFactor = WEIGHT_CONVERSIONS.get(fromUnit);
        Double toFactor = WEIGHT_CONVERSIONS.get(toUnit);
        
        if (fromFactor == null || toFactor == null) {
            return null;
        }
        
        // Convert to base unit (grams), then to target unit
        Double inGrams = quantity * fromFactor;
        return inGrams / toFactor;
    }

    /**
     * Converts volume from one unit to another
     */
    private Double convertVolume(Double quantity, String fromUnit, String toUnit) {
        Double fromFactor = VOLUME_CONVERSIONS.get(fromUnit);
        Double toFactor = VOLUME_CONVERSIONS.get(toUnit);
        
        if (fromFactor == null || toFactor == null) {
            return null;
        }
        
        // Convert to base unit (ml), then to target unit
        Double inMl = quantity * fromFactor;
        return inMl / toFactor;
    }

    /**
     * Converts quantity to canonical base unit (grams for weight, ml for volume, pieces for count).
     * 
     * @return Array [quantityInBase, 1=weight/2=volume/3=count], or null if unit unknown
     */
    public double[] convertToCanonical(Double quantity, String unit) {
        if (quantity == null || unit == null) return null;
        String norm = normalizeUnit(unit);
        if (isWeightUnit(norm)) {
            Double inGrams = convertWeight(quantity, norm, "grams");
            return inGrams != null ? new double[]{inGrams, 1} : null;
        }
        if (isVolumeUnit(norm)) {
            Double inMl = convertVolume(quantity, norm, "ml");
            return inMl != null ? new double[]{inMl, 2} : null;
        }
        if (isCountUnit(norm)) return new double[]{quantity, 3};
        return new double[]{quantity, 3}; // treat unknown as count
    }

    /**
     * Converts from canonical (grams/ml/pieces) to a human-friendly display unit.
     */
    public String formatDisplayUnit(double canonicalQty, int type) {
        if (type == 1) return canonicalQty >= 1000 ? "kg" : "grams";
        if (type == 2) return canonicalQty >= 1000 ? "liters" : "ml";
        return "pieces";
    }

    /**
     * Converts canonical quantity to display unit and returns formatted quantity.
     */
    public double convertFromCanonicalToDisplay(double canonicalQty, int type) {
        if (type == 1 && canonicalQty >= 1000) return canonicalQty / 1000;
        if (type == 2 && canonicalQty >= 1000) return canonicalQty / 1000;
        return canonicalQty;
    }

    public boolean isWeightUnitPublic(String unit) { return isWeightUnit(normalizeUnit(unit)); }
    public boolean isVolumeUnitPublic(String unit) { return isVolumeUnit(normalizeUnit(unit)); }
    public boolean isCountUnitPublic(String unit) { return isCountUnit(normalizeUnit(unit)); }

    /**
     * Checks if two units are compatible (can be converted)
     */
    public boolean areUnitsCompatible(String unit1, String unit2) {
        if (unit1 == null || unit2 == null) {
            return false;
        }
        
        String normalized1 = normalizeUnit(unit1);
        String normalized2 = normalizeUnit(unit2);
        
        if (normalized1.equals(normalized2)) {
            return true;
        }
        
        return (isWeightUnit(normalized1) && isWeightUnit(normalized2)) ||
               (isVolumeUnit(normalized1) && isVolumeUnit(normalized2)) ||
               (isCountUnit(normalized1) && isCountUnit(normalized2));
    }
}
