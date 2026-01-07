package com.mealcraft.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User Entity
 * 
 * Represents a user account in the MealCraft system.
 * Stores user authentication credentials and profile information.
 * 
 * @author MealCraft Team
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    /**
     * Unique identifier for the user
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's email address (used for login
     * Must be unique and valid email format
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Encrypted password using BCrypt
     * Minimum 6 characters
     */
    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * User's first name
     * Required field
     */
    @Column(nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    /**
     * User's last name
     * Required field
     */
    @Column(nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    /**
     * User's age (optional field)
     */
    @Column
    private Integer age;

    /**
     * Timestamp when user account was created
     * Automatically set on creation
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when user account was last modified
     * Automatically updated on modification
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * One-to-many relationship with pantry items
     * Cascade delete: if user is deleted, all their pantry items are deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PantryItem> pantryItems = new ArrayList<>();

    /**
     * One-to-many relationship with saved recipes
     * Cascade delete: if user is deleted, all their saved recipes are deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedRecipe> savedRecipes = new ArrayList<>();

    /**
     * One-to-many relationship with meal plans
     * Cascade delete: if user is deleted, all their meal plans are deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPlan> mealPlans = new ArrayList<>();

    /**
     * One-to-many relationship with shopping list items
     * Cascade delete: if user is deleted, all their shopping list items are deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    /**
     * Helper method to get user's full name
     * @return Full name (firstName + lastName)
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Helper method to get user's initials for profile icon
     * @return Initials (e.g., "JD" for John Doe)
     */
    public String getInitials() {
        String firstInitial = firstName != null && !firstName.isEmpty() 
            ? String.valueOf(firstName.charAt(0)).toUpperCase() 
            : "";
        String lastInitial = lastName != null && !lastName.isEmpty() 
            ? String.valueOf(lastName.charAt(0)).toUpperCase() 
            : "";
        return firstInitial + lastInitial;
    }
}



