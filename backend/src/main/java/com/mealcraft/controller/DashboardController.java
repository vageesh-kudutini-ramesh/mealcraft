package com.mealcraft.controller;

import com.mealcraft.dto.DashboardStatsDTO;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard Controller
 * 
 * Handles dashboard statistics and alerts endpoints.
 * Provides comprehensive dashboard data including expiring items, alerts, and statistics.
 * 
 * @author MealCraft Team
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets comprehensive dashboard statistics
     * 
     * GET /api/dashboard/stats
     * 
     * @param authentication Spring Security authentication object
     * @return DashboardStatsDTO with all dashboard information
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        DashboardStatsDTO stats = dashboardService.getDashboardStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}




