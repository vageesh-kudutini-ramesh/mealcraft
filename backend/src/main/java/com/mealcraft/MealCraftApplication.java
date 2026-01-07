package com.mealcraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for MealCraft Backend API
 * 
 * MealCraft is a meal planning and pantry management application
 * that helps users reduce food waste and make smarter cooking decisions.
 * 
 * @author MealCraft Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing // Enables automatic timestamp management (@CreatedDate, @LastModifiedDate)
@EnableScheduling // Enables scheduled tasks (for expiration notifications)
public class MealCraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealCraftApplication.class, args);
    }
}



