package com.mealcraft.repository;

import com.mealcraft.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 * 
 * Provides CRUD operations and custom query methods for User management.
 * Extends JpaRepository for standard database operations.
 * 
 * @author MealCraft Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address
     * Used for login and registration validation
     * 
     * @param email User's email address
     * @return Optional containing User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email
     * Used to prevent duplicate email registrations
     * 
     * @param email Email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}




