package com.mealcraft.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.MealPlanPreferencesDTO;
import com.mealcraft.model.MealPlanPreferences;
import com.mealcraft.model.User;
import com.mealcraft.repository.MealPlanPreferencesRepository;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MealPlanPreferencesService {

    @Autowired
    private MealPlanPreferencesRepository preferencesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public MealPlanPreferencesDTO getPreferences(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        MealPlanPreferences prefs = preferencesRepository.findByUser_Id(userId).orElse(null);
        if (prefs == null) {
            return defaultPreferencesDTO();
        }
        MealPlanPreferencesDTO dto = new MealPlanPreferencesDTO();
        try {
            dto.setPatterns(prefs.getPatternsJson() != null
                ? objectMapper.readValue(prefs.getPatternsJson(), new TypeReference<List<Map<String, Object>>>() {})
                : new ArrayList<>());
        } catch (Exception e) {
            dto.setPatterns(new ArrayList<>());
        }
        try {
            dto.setDietaryRules(prefs.getDietaryRulesJson() != null
                ? objectMapper.readValue(prefs.getDietaryRulesJson(), new TypeReference<Map<String, Object>>() {})
                : defaultDietaryRules());
        } catch (Exception e) {
            dto.setDietaryRules(defaultDietaryRules());
        }
        return dto;
    }

    public MealPlanPreferencesDTO savePreferences(Long userId, MealPlanPreferencesDTO dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        MealPlanPreferences prefs = preferencesRepository.findByUser_Id(userId).orElse(null);
        if (prefs == null) {
            prefs = new MealPlanPreferences();
            prefs.setUser(user);
        }
        try {
            prefs.setPatternsJson(objectMapper.writeValueAsString(dto.getPatterns() != null ? dto.getPatterns() : new ArrayList<>()));
        } catch (Exception e) {
            prefs.setPatternsJson("[]");
        }
        try {
            prefs.setDietaryRulesJson(objectMapper.writeValueAsString(dto.getDietaryRules() != null ? dto.getDietaryRules() : defaultDietaryRules()));
        } catch (Exception e) {
            prefs.setDietaryRulesJson("{}");
        }
        prefs.setUpdatedAt(LocalDateTime.now());
        preferencesRepository.save(prefs);
        return getPreferences(userId);
    }

    private static MealPlanPreferencesDTO defaultPreferencesDTO() {
        MealPlanPreferencesDTO dto = new MealPlanPreferencesDTO();
        dto.setPatterns(new ArrayList<>());
        dto.setDietaryRules(defaultDietaryRules());
        return dto;
    }

    private static Map<String, Object> defaultDietaryRules() {
        Map<String, Object> r = new HashMap<>();
        r.put("noGluten", false);
        r.put("minVegetarianDinnersPerWeek", 0);
        r.put("maxCaloriesPerDinner", null);
        return r;
    }
}
