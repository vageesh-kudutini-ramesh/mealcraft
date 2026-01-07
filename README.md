# MealCraft

**Plan smarter. Eat better.**

MealCraft is a comprehensive meal planning and pantry management application that helps home cooks reduce food waste, save time, and make smarter cooking decisions.

## Problem Statement

Home cooks face four major daily challenges:
1. **Decision Fatigue**: "What should I cook today?" - This question causes stress and wastes time every single day.
2. **Food Waste**: People buy groceries but forget what they have, leading to expired items, duplicate purchases, and unused ingredients.
3. **Lack of Recipe Knowledge**: Users don't have a personal collection of tried recipes or don't know what dishes can be made with ingredients they already have.
4. **Expiration Blindness**: Users don't get timely daily alerts about items approaching expiration, resulting in waste.

## Solution

MealCraft provides a unified platform that:
- ✅ Intelligently suggests recipes based on available pantry ingredients and expiration dates
- ✅ Tracks pantry inventory with expiration dates
- ✅ Provides daily expiry notifications (starting 5 days before expiration)
- ✅ Allows users to save suggested recipes they like for future reference
- ✅ Enables drag-and-drop weekly meal planning with saved recipes
- ✅ Auto-generates shopping lists from meal plans
- ✅ Reduces food waste and decision-making time
- ✅ Provides profile management for personalized experience

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17+
- **Security**: Spring Security with JWT authentication
- **Database**: PostgreSQL 15+
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **External API**: TheMealDB API (recipe suggestions - completely free, no API key needed)

### Frontend
- **Framework**: React 18+
- **Build Tool**: Vite
- **Styling**: Tailwind CSS 3.x
- **Routing**: React Router 6.x
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Date Handling**: date-fns
- **Drag & Drop**: @dnd-kit/core

## Project Structure

```
mealcraft/
├── backend/              # Spring Boot backend API
│   ├── src/
│   │   ├── main/java/com/mealcraft/
│   │   │   ├── config/      # Configuration classes
│   │   │   ├── controller/  # REST controllers
│   │   │   ├── dto/         # Data Transfer Objects
│   │   │   ├── model/       # Entity models
│   │   │   ├── repository/ # JPA repositories
│   │   │   ├── security/    # Security configuration
│   │   │   └── service/     # Business logic
│   │   └── resources/
│   │       └── application.properties
│   └── pom.xml
│
├── frontend/             # React frontend application
│   ├── src/
│   │   ├── components/   # Reusable components
│   │   ├── contexts/     # React Context providers
│   │   ├── pages/         # Page components
│   │   ├── utils/         # Utility functions
│   │   └── App.jsx
│   ├── package.json
│   └── vite.config.js
│
└── MealCraft_Initial_Document.txt  # Requirements document
```

## Quick Start

### Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** installed
3. **Node.js 18+** installed
4. **PostgreSQL 15+** installed and running
5. **TheMealDB API** (no setup needed - completely free!)

### Backend Setup

1. **Create PostgreSQL database**:
   ```sql
   CREATE DATABASE mealcraft_db;
   ```

2. **Configure database** in `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/mealcraft_db
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   themealdb.api.base-url=https://www.themealdb.com/api/json/v1/1
   ```

3. **Build and run backend**:
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

   Backend will run on `http://localhost:8080`

### Frontend Setup

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Start development server**:
   ```bash
   npm run dev
   ```

   Frontend will run on `http://localhost:5173`

## Features

### Core Features

1. **User Authentication & Authorization**
   - User registration with email and password
   - Secure login with JWT token-based authentication
   - Password encryption using BCrypt
   - Session persistence
   - Protected routes

2. **User Profile Management**
   - Profile icon with user initials
   - Editable profile fields (first name, last name, age)
   - Color-coded profile icons

3. **Pantry Inventory Management**
   - Four categories: Fruits & Vegetables, Dairy Products, Pantry Staples, Condiments & Spices
   - Add items with quantity, unit, category, and expiration date
   - Expiration tracking with visual indicators (🟢 Fresh, 🟡 Expiring Soon, 🔴 Expired)
   - Low-stock alerts
   - Bulk operations (delete expired items)

4. **Recipe Suggestion System**
   - Intelligent recipe suggestions based on pantry ingredients
   - Integration with TheMealDB API (completely free, no API key needed)
   - Parallel API calls for optimal performance
   - Smart ingredient matching with normalization
   - Priority scoring for recipes using expiring ingredients
   - Match percentage calculation
   - Meal type filtering (Breakfast, Lunch, Dinner, All)
   - Recipe caching for improved performance

5. **Saved Recipes Management**
   - Save suggested recipes to personal collection
   - Search and filter saved recipes
   - Edit and customize saved recipes
   - View recipe details with ingredients and instructions

6. **Daily Expiration Notifications**
   - Alerts for items expiring in 1-5 days
   - Expired items alerts
   - Dashboard notification panel
   - Notification icon with count badge

7. **Weekly Meal Planning**
   - Calendar grid view (7 days × 3 meals)
   - Drag-and-drop recipe assignment
   - Quick-add recipes
   - Recipe snapshot (retains data even if saved recipe deleted)

8. **Shopping List Management**
   - Auto-generate from weekly meal plans
   - Manual item addition
   - Mark items as purchased
   - Clear purchased items
   - Suggested expiration dates

9. **Enhanced Dashboard**
   - Expiring items alert panel
   - Expired items alert panel
   - Low-stock alert panel
   - Quick action buttons
   - Statistics (total items, saved recipes, today's meals)
   - Recent saved recipes

## API Documentation

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### User Profile
- `GET /api/users/profile` - Get current user profile
- `PUT /api/users/profile` - Update user profile

### Pantry Management
- `GET /api/pantry` - Get all pantry items
- `GET /api/pantry/expiring` - Get expiring items
- `GET /api/pantry/expired` - Get expired items
- `GET /api/pantry/low-stock` - Get low-stock items
- `POST /api/pantry` - Create pantry item
- `PUT /api/pantry/{id}` - Update pantry item
- `DELETE /api/pantry/{id}` - Delete pantry item

### Recipe Management
- `POST /api/recipes/suggest` - Get recipe suggestions
- `GET /api/recipes/saved` - Get saved recipes
- `POST /api/recipes/saved` - Save recipe
- `PUT /api/recipes/saved/{id}` - Update saved recipe
- `DELETE /api/recipes/saved/{id}` - Delete saved recipe

### Meal Planning
- `GET /api/meal-plans/week` - Get weekly meal plan
- `POST /api/meal-plans` - Create meal plan
- `PUT /api/meal-plans/{id}` - Update meal plan
- `DELETE /api/meal-plans/{id}` - Delete meal plan

### Shopping List
- `GET /api/shopping-list` - Get shopping list items
- `POST /api/shopping-list/generate` - Generate from meal plan
- `POST /api/shopping-list` - Create shopping list item
- `POST /api/shopping-list/{id}/purchase` - Mark as purchased
- `DELETE /api/shopping-list/{id}` - Delete shopping list item

## Development

### Code Structure

The project follows industry-standard practices:
- **Clean Architecture**: Separation of concerns (controllers, services, repositories)
- **RESTful API Design**: Standard HTTP methods and status codes
- **Comprehensive Comments**: All classes and methods are well-documented
- **Error Handling**: Proper exception handling and error responses
- **Validation**: Input validation at both frontend and backend
- **Security**: JWT authentication, password encryption, protected routes

### Contributing

1. Follow the existing code structure and naming conventions
2. Add comprehensive comments to new code
3. Test all features before committing
4. Update documentation as needed
5. Use meaningful commit messages following conventional commit format

## License

This project is private and proprietary.

## Contact

For questions or support, please contact; 

LinkedIn : https://www.linkedin.com/in/vageesh-kudutini-ramesh/
Email: vageesh2001@gmail.com
---

**MealCraft** - Plan smarter. Eat better. 🍳


