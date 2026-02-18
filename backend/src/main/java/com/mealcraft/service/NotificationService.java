package com.mealcraft.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealcraft.dto.NotificationDTO;
import com.mealcraft.dto.RecipeDTO;
import com.mealcraft.dto.RecipeSuggestionRequest;
import com.mealcraft.model.DismissedNotification;
import com.mealcraft.model.PantryItem;
import com.mealcraft.model.SavedRecipe;
import com.mealcraft.model.User;
import com.mealcraft.repository.DismissedNotificationRepository;
import com.mealcraft.repository.MealPlanRepository;
import com.mealcraft.repository.PantryItemRepository;
import com.mealcraft.repository.SavedRecipeRepository;
import com.mealcraft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates all in-app notifications (bell icon).
 * Computes notifications from pantry, meal plan, shopping list, recipes.
 */
@Service
@Transactional
public class NotificationService {

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @Autowired
    private DismissedNotificationRepository dismissedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingListService shoppingListService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int EXPIRING_DAYS = 5;
    private static final String TYPE_PANTRY_EXPIRING = "PANTRY_EXPIRING";
    private static final String TYPE_PANTRY_EXPIRING_RECIPE = "PANTRY_EXPIRING_RECIPE";
    private static final String TYPE_MEAL_PLAN_SUMMARY = "MEAL_PLAN_SUMMARY";
    private static final String TYPE_MEAL_PLAN_EMPTY_TOMORROW = "MEAL_PLAN_EMPTY_TOMORROW";
    private static final String TYPE_SHOPPING_SYNC = "SHOPPING_SYNC";
    private static final String TYPE_UNUSED_RECIPES = "UNUSED_RECIPES";
    private static final String TYPE_EMPTY_PANTRY = "EMPTY_PANTRY";
    private static final String TYPE_LOW_STOCK = "LOW_STOCK";
    private static final String TYPE_STREAK = "STREAK";
    private static final String TYPE_PLANNING_PROMPT = "PLANNING_PROMPT";
    private static final String TYPE_MORNING_BRIEF = "MORNING_BRIEF";
    private static final String TYPE_WEEK_AHEAD = "WEEK_AHEAD";

    /**
     * Fetches all active notifications for the user (excluding dismissed).
     */
    public List<NotificationDTO> getNotifications(Long userId) {
        Set<String> dismissedPantry = new HashSet<>(dismissedRepository.findDismissedReferenceIds(userId, TYPE_PANTRY_EXPIRING));
        Set<String> dismissedRecipeMatch = new HashSet<>(dismissedRepository.findDismissedReferenceIds(userId, TYPE_PANTRY_EXPIRING_RECIPE));
        Set<String> dismissedGeneric = new HashSet<>();
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_MEAL_PLAN_SUMMARY));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_MEAL_PLAN_EMPTY_TOMORROW));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_SHOPPING_SYNC));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_UNUSED_RECIPES));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_EMPTY_PANTRY));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_STREAK));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_PLANNING_PROMPT));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_MORNING_BRIEF));
        dismissedGeneric.addAll(dismissedRepository.findDismissedReferenceIds(userId, TYPE_WEEK_AHEAD));
        Set<String> dismissedLowStock = new HashSet<>(dismissedRepository.findDismissedReferenceIds(userId, TYPE_LOW_STOCK));

        List<NotificationDTO> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        if (weekEnd.isBefore(today)) weekEnd = weekEnd.plusWeeks(1);
        LocalDate weekStart = weekEnd.minusDays(6);

        // 1. Pantry expiring soon (per item)
        LocalDate expEnd = today.plusDays(EXPIRING_DAYS);
        List<PantryItem> expiring = pantryItemRepository.findExpiringItems(userId, today, expEnd);
        for (PantryItem p : expiring) {
            String ref = String.valueOf(p.getId());
            if (dismissedPantry.contains(ref)) continue;
            long days = p.getDaysUntilExpiry();
            String sub = days == 0 ? "Expires today!" : days == 1 ? "Expires tomorrow" : days + " days left";
            out.add(new NotificationDTO(
                TYPE_PANTRY_EXPIRING + "_" + ref,
                TYPE_PANTRY_EXPIRING,
                "Expiring soon",
                p.getItemName() + " is expiring soon.",
                "/pantry",
                ref,
                days <= 1 ? "warning" : "info",
                "clock",
                sub,
                null
            ));
        }

        // 2. Expiring + recipe match (smart alert) - only for items expiring in 1-3 days, limit to 3
        RecipeSuggestionRequest suggestReq = new RecipeSuggestionRequest();
        suggestReq.setArea(null);
        suggestReq.setDiet("ALL");
        int recipeMatchCount = 0;
        for (PantryItem p : expiring) {
            if (recipeMatchCount >= 3) break;
            long days = p.getDaysUntilExpiry();
            if (days < 1 || days > 3) continue;
            String ref = "recipe_" + p.getId();
            if (dismissedRecipeMatch.contains(ref)) continue;
            try {
                List<RecipeDTO> matches = recipeService.suggestRecipes(userId, suggestReq, 0);
                List<RecipeDTO> using = matches.stream().limit(3).collect(Collectors.toList());
                if (!using.isEmpty()) {
                    String sub = p.getItemName() + " – " + (days == 1 ? "tomorrow" : days + " days");
                    out.add(new NotificationDTO(
                        TYPE_PANTRY_EXPIRING_RECIPE + "_" + ref,
                        TYPE_PANTRY_EXPIRING_RECIPE,
                        "Use it up!",
                        "Your " + p.getItemName() + " expires in " + days + " day" + (days > 1 ? "s" : "") + ". Try these recipes.",
                        "/recipes",
                        ref,
                        "tip",
                        "chef-hat",
                        sub,
                        using
                    ));
                    recipeMatchCount++;
                }
            } catch (Exception ignore) {}
        }

        // 3. Meal plan summary
        List<com.mealcraft.model.MealPlan> weekPlans = mealPlanRepository.findWeeklyMealPlans(userId, weekStart, weekEnd);
        int weekCount = weekPlans.size();
        String weekRef = "week_" + weekStart;
        if (!dismissedGeneric.contains(weekRef) && weekCount > 0) {
            out.add(new NotificationDTO(
                TYPE_MEAL_PLAN_SUMMARY + "_" + weekRef,
                TYPE_MEAL_PLAN_SUMMARY,
                "Meal plan status",
                weekCount + " meal" + (weekCount == 1 ? "" : "s") + " planned for this week.",
                "/meal-plan",
                weekRef,
                "success",
                "calendar",
                null,
                null
            ));
        }

        // 4. No meals planned for tomorrow
        List<com.mealcraft.model.MealPlan> tomorrowPlans = mealPlanRepository.findByUserIdAndDate(userId, tomorrow);
        String tomRef = "tomorrow_" + tomorrow;
        if (!dismissedGeneric.contains(tomRef) && tomorrowPlans.isEmpty() && weekCount > 0) {
            out.add(new NotificationDTO(
                TYPE_MEAL_PLAN_EMPTY_TOMORROW + "_" + tomRef,
                TYPE_MEAL_PLAN_EMPTY_TOMORROW,
                "Tomorrow is empty",
                "No meals planned for tomorrow. Add something?",
                "/meal-plan",
                tomRef,
                "info",
                "calendar",
                null,
                null
            ));
        }

        // 5. Shopping list sync
        try {
            int missing = shoppingListService.countMealPlanIngredientsNotOnShoppingList(userId, weekStart, weekEnd);
            String shopRef = "sync_" + weekStart;
            if (!dismissedGeneric.contains(shopRef) && missing > 0 && weekCount > 0) {
                out.add(new NotificationDTO(
                    TYPE_SHOPPING_SYNC + "_" + shopRef,
                    TYPE_SHOPPING_SYNC,
                    "Shopping list sync",
                    missing + " item" + (missing == 1 ? "" : "s") + " from your meal plan " + (missing == 1 ? "isn't" : "aren't") + " on your shopping list.",
                    "/shopping-list",
                    shopRef,
                    "warning",
                    "shopping-cart",
                    null,
                    null
                ));
            }
        } catch (Exception ignore) {}

        // 6. Unused recipes (saved but never in meal plan)
        List<SavedRecipe> unusedRecipes = savedRecipeRepository.findUnusedRecipes(userId);
        int unusedCount = unusedRecipes.size();
        if (!dismissedGeneric.contains("unused") && unusedCount >= 3) {
            out.add(new NotificationDTO(
                TYPE_UNUSED_RECIPES + "_unused",
                TYPE_UNUSED_RECIPES,
                "Uncooked treasures",
                "You've saved " + unusedCount + " recipes you haven't tried yet. Plan them this week!",
                "/recipes",
                "unused",
                "tip",
                "book-open",
                null,
                null
            ));
        }

        // 7. Empty pantry tip
        List<PantryItem> pantry = pantryItemRepository.findByUserId(userId);
        if (!dismissedGeneric.contains("empty") && pantry.isEmpty()) {
            out.add(new NotificationDTO(
                TYPE_EMPTY_PANTRY + "_empty",
                TYPE_EMPTY_PANTRY,
                "Get started",
                "Add a few staples to your pantry for personalized recipe suggestions.",
                "/pantry",
                "empty",
                "info",
                "package",
                null,
                null
            ));
        }

        // 8. Low stock
        List<PantryItem> lowStock = pantryItemRepository.findLowStockItems(userId);
        for (PantryItem p : lowStock) {
            String ref = "low_" + p.getId();
            if (dismissedLowStock.contains(ref)) continue;
            out.add(new NotificationDTO(
                TYPE_LOW_STOCK + "_" + ref,
                TYPE_LOW_STOCK,
                "Running low",
                p.getItemName() + " is below your threshold.",
                "/pantry",
                ref,
                "warning",
                "alert-circle",
                null,
                null
            ));
        }

        // 9. Streak – count consecutive weeks with plans, starting from current week going backwards
        // This ensures "1 week planned" never shows as "2 weeks" (e.g. no double-count from Sunday/Monday boundary)
        LocalDate cursorMonday = today.with(DayOfWeek.MONDAY);
        int consecutiveWeeks = 0;
        for (int i = 0; i < 8; i++) {
            LocalDate weekEndDate = cursorMonday.plusDays(6);
            List<com.mealcraft.model.MealPlan> plansInWeek = mealPlanRepository.findWeeklyMealPlans(userId, cursorMonday, weekEndDate);
            if (plansInWeek.isEmpty()) break;
            consecutiveWeeks++;
            cursorMonday = cursorMonday.minusWeeks(1);
        }
        if (!dismissedGeneric.contains("streak") && consecutiveWeeks >= 2) {
            out.add(new NotificationDTO(
                TYPE_STREAK + "_streak",
                TYPE_STREAK,
                "You're on a roll!",
                "You've planned meals for " + consecutiveWeeks + " weeks in a row. Keep it up!",
                "/meal-plan",
                "streak",
                "success",
                "trending-up",
                null,
                null
            ));
        }

        // 10. Planning prompt (Sunday)
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY && !dismissedGeneric.contains("sunday_" + today.toString())) {
            out.add(new NotificationDTO(
                TYPE_PLANNING_PROMPT + "_sunday_" + today,
                TYPE_PLANNING_PROMPT,
                "Plan your week",
                "It's Sunday! Plan your meals for the week ahead.",
                "/meal-plan",
                "sunday_" + today.toString(),
                "info",
                "calendar",
                null,
                null
            ));
        }

        // 11. Morning brief (compact – only if we have multiple things to say)
        int todayMeals = mealPlanRepository.findByUserIdAndDate(userId, today).size();
        int expiringCount = expiring.size();
        String briefRef = "brief_" + today;
        if (!dismissedGeneric.contains(briefRef) && (todayMeals > 0 || expiringCount > 0)) {
            StringBuilder msg = new StringBuilder();
            if (todayMeals > 0) msg.append(todayMeals).append(" meal").append(todayMeals > 1 ? "s" : "").append(" planned today");
            if (todayMeals > 0 && expiringCount > 0) msg.append(", ");
            if (expiringCount > 0) msg.append(expiringCount).append(" item").append(expiringCount > 1 ? "s" : "").append(" expiring soon");
            msg.append(".");
            out.add(new NotificationDTO(
                TYPE_MORNING_BRIEF + "_" + briefRef,
                TYPE_MORNING_BRIEF,
                "Today at a glance",
                msg.toString(),
                "/meal-plan",
                briefRef,
                "info",
                "sun",
                null,
                null
            ));
        }

        // 12. Week ahead
        LocalDate nextWeekEnd = weekEnd.plusDays(7);
        LocalDate nextWeekStart = weekStart.plusDays(7);
        List<PantryItem> nextWeekExpiring = pantryItemRepository.findExpiringItems(userId, today.plusDays(1), today.plusDays(7));
        int nextWeekMeals = mealPlanRepository.findWeeklyMealPlans(userId, nextWeekStart, nextWeekEnd).size();
        String aheadRef = "ahead_" + weekStart;
        if (!dismissedGeneric.contains(aheadRef) && (nextWeekExpiring.size() > 0 || nextWeekMeals > 0)) {
            StringBuilder m = new StringBuilder();
            if (nextWeekExpiring.size() > 0) m.append(nextWeekExpiring.size()).append(" items expiring");
            if (nextWeekExpiring.size() > 0 && nextWeekMeals > 0) m.append(", ");
            if (nextWeekMeals > 0) m.append(nextWeekMeals).append(" meals planned");
            m.append(" for the week ahead.");
            out.add(new NotificationDTO(
                TYPE_WEEK_AHEAD + "_" + aheadRef,
                TYPE_WEEK_AHEAD,
                "Week ahead",
                m.toString(),
                "/meal-plan",
                aheadRef,
                "info",
                "calendar",
                null,
                null
            ));
        }

        return out;
    }

    /**
     * Dismisses a notification so it won't show again.
     */
    public void dismiss(Long userId, String notificationType, String referenceId) {
        if (dismissedRepository.existsByUser_IdAndNotificationTypeAndReferenceId(userId, notificationType, referenceId))
            return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        DismissedNotification d = new DismissedNotification();
        d.setUser(user);
        d.setNotificationType(notificationType);
        d.setReferenceId(referenceId);
        dismissedRepository.save(d);
    }
}
