package com.mealcraft.controller;

import com.mealcraft.dto.NotificationDTO;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import com.mealcraft.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API for in-app notifications (bell icon).
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (!(p instanceof UserDetails)) return null;
        String email = ((UserDetails) p).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        List<NotificationDTO> notifications = notificationService.getNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/dismiss")
    public ResponseEntity<Void> dismiss(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String type = body.get("type");
        String referenceId = body.get("referenceId");
        if (type == null || referenceId == null) {
            return ResponseEntity.badRequest().build();
        }
        notificationService.dismiss(user.getId(), type, referenceId);
        return ResponseEntity.ok().build();
    }
}
