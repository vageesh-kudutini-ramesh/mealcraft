package com.mealcraft.service;

import com.mealcraft.dto.UserProfileDTO;
import com.mealcraft.model.User;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service
 * 
 * Handles user profile management operations.
 * Provides methods for retrieving and updating user profile information.
 * 
 * @author MealCraft Team
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets user profile by email
     * 
     * @param email User's email address
     * @return UserProfileDTO containing user profile information
     * @throws UsernameNotFoundException if user not found
     */
    public UserProfileDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return mapToDTO(user);
    }

    /**
     * Gets user profile by user ID
     * 
     * @param userId User's ID
     * @return UserProfileDTO containing user profile information
     * @throws RuntimeException if user not found
     */
    public UserProfileDTO getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return mapToDTO(user);
    }

    /**
     * Updates user profile
     * 
     * @param email User's email address
     * @param profileDTO Updated profile information
     * @return Updated UserProfileDTO
     * @throws UsernameNotFoundException if user not found
     */
    public UserProfileDTO updateUserProfile(String email, UserProfileDTO profileDTO) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Update editable fields (email cannot be changed)
        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setAge(profileDTO.getAge());

        // Save updated user
        user = userRepository.save(user);

        return mapToDTO(user);
    }

    /**
     * Maps User entity to UserProfileDTO
     */
    private UserProfileDTO mapToDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setAge(user.getAge());
        dto.setFullName(user.getFullName());
        dto.setInitials(user.getInitials());
        return dto;
    }
}




