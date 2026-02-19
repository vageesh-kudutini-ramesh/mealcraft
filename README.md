# MealCraft

**Plan smarter. Eat better.**

MealCraft is a full-stack meal planning and pantry management application that helps home cooks reduce food waste, save time, and make smarter cooking decisions.

---

## Problem Statement

Home cooks face common daily challenges:

1. **Decision fatigue** – "What should I cook today?" causes stress and wastes time.
2. **Food waste** – Forgetting what's in the pantry leads to expired items and duplicate purchases.
3. **Recipe discovery** – Not knowing what dishes can be made with available ingredients.
4. **Expiration blindness** – Missing alerts for items approaching expiration.

## Solution

MealCraft provides a unified platform that:

- Tracks pantry inventory with expiration dates and low-stock thresholds
- Suggests recipes based on pantry ingredients (via Spoonacular API)
- Lets you save recipes, plan weekly meals, and auto-generate shopping lists
- Sends notifications for expiring and expired items
- Supports dietary preferences and meal planning patterns

---

## Technology Stack

### Backend

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.0 |
| Language | Java 17+ |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Build | Maven |
| Recipe API | [Spoonacular API](https://spoonacular.com/food-api) (free tier: 150 requests/day) |

### Frontend

| Component | Technology |
|-----------|------------|
| Framework | React 18+ |
| Build | Vite |
| Styling | Tailwind CSS |
| Routing | React Router 6.x |
| HTTP | Axios |
| Icons | Lucide React |
| Date handling | date-fns |
| Drag & Drop | @dnd-kit/core |
| PDF export | jsPDF + jspdf-autotable |

---

## Project Structure

```
mealcraft/
├── backend/                    # Spring Boot API
│   ├── src/main/java/com/mealcraft/
│   │   ├── config/             # RestTemplate, etc.
│   │   ├── controller/         # REST controllers
│   │   ├── dto/               # Data transfer objects
│   │   ├── model/             # JPA entities
│   │   ├── repository/        # Data access
│   │   ├── security/         # JWT, BCrypt
│   │   └── service/           # Business logic
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── application-local.properties.example   # Template for secrets
│   └── pom.xml
│
├── frontend/                   # React SPA
│   ├── src/
│   │   ├── components/       # Reusable UI
│   │   ├── contexts/         # Auth, Notifications
│   │   ├── data/             # Cuisine ingredients, occasions
│   │   ├── pages/            # Page components
│   │   └── utils/            # Axios, date helpers
│   ├── package.json
│   └── vite.config.js
│
├── .gitignore
└── README.md
```

---

## Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Node.js 18+**
- **PostgreSQL 15+**
- **Spoonacular API key** – [Get a free key](https://spoonacular.com/food-api/console)

---

## Quick Start

### 1. Create Database

```sql
CREATE DATABASE mealcraft_db;
```

### 2. Backend Setup

```bash
cd backend

# Create local config (secrets – never committed)
cp application-local.properties.example application-local.properties

# Edit application-local.properties and add:
#   - spring.datasource.password    (your PostgreSQL password)
#   - jwt.secret                    (random 64+ char string)
#   - spoonacular.api.key           (from Spoonacular)

# Build and run
mvn clean install
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`

### 3. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

---

## Features

### 1. Authentication

- Register with email and password
- Login with JWT-based sessions
- BCrypt password encryption
- Protected routes for logged-in users

### 2. User Profile

- Editable first name, last name, age
- Profile avatar with color-coded initials
- Dropdown menu (Profile, Logout) in navbar

### 3. Pantry Management

- Add items with quantity, unit, category, expiration date, low-stock threshold
- Categories: Fruits & Vegetables, Dairy, Pantry Staples, Condiments & Spices
- **Quick Add from Suggestions** – pick a cuisine (Indian, Italian, Mexican, etc.), add ingredients with one click
- Filters: All Items | Expiring Soon | Expired | Low Stock
- Status badges: Fresh, Expiring Soon, Expired, Low Stock
- Edit, delete, and bulk delete expired items

### 4. Recipe Suggestions

- Suggest recipes from pantry ingredients (Spoonacular API)
- Browse/discover recipes by cuisine, diet, query
- Cuisine areas (Italian, Indian, Mexican, etc.)
- Diet filters (vegetarian, vegan, etc.)
- Match percentage for pantry-based suggestions
- **Cook Recipe** – deduct used ingredients from pantry
- Recipe caching to optimize API usage

### 5. Saved Recipes

- Save suggested recipes to a personal collection
- Search saved recipes
- View details (ingredients, instructions)
- Add ingredients to shopping list
- Edit and delete saved recipes

### 6. Notifications

- Dashboard alerts for expiring items (1–5 days before)
- Expired items alerts
- Low-stock alerts
- Notification bell with count badge
- Dismissible notifications

### 7. Weekly Meal Planning

- 7-day × 3-meal grid (Breakfast, Lunch, Dinner)
- Drag-and-drop recipe assignment
- Add from saved recipes or discover new ones
- Batch-add same recipe to multiple slots
- **Dietary preferences** – no gluten, no dairy, min vegetarian dinners, etc.
- Leftover suggestions when removing a plan
- Apply/revert patterns (e.g., repeat last week)
- **Export to PDF** – meal plan and optional shopping list
- Sync meal plan with shopping list

### 8. Shopping List

- Generate from weekly meal plan (with confirm step)
- Manual add items
- Mark as purchased / clear purchased
- Show items to buy vs purchased
- Incremental add when recipe already in list

---

## API Overview

### Authentication (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Login |

### User

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/profile` | Get profile |
| PUT | `/api/users/profile` | Update profile |

### Pantry

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pantry` | All items |
| GET | `/api/pantry/expiring` | Expiring soon |
| GET | `/api/pantry/expired` | Expired |
| GET | `/api/pantry/low-stock` | Low stock |
| POST | `/api/pantry` | Add item |
| PUT | `/api/pantry/{id}` | Update item |
| DELETE | `/api/pantry/{id}` | Delete item |
| POST | `/api/pantry/{id}/use` | Use quantity |

### Recipes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/recipes/suggest` | Suggest from pantry |
| GET | `/api/recipes/discover` | Browse by filters |
| GET | `/api/recipes/areas` | Cuisine areas |
| POST | `/api/recipes/cook` | Cook (deduct pantry) |
| GET | `/api/recipes/saved` | Saved recipes |
| POST | `/api/recipes/saved` | Save recipe |
| GET | `/api/recipes/enhance/{id}` | Recipe details |

### Meal Plans

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/meal-plans/week` | Week view |
| POST | `/api/meal-plans` | Create plan |
| PUT | `/api/meal-plans/{id}` | Update plan |
| DELETE | `/api/meal-plans/{id}` | Delete plan |
| GET | `/api/meal-plans/preferences` | Dietary rules |
| GET | `/api/meal-plans/export` | PDF export |

### Shopping List

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/shopping-list` | All items |
| POST | `/api/shopping-list/generate` | From meal plan |
| POST | `/api/shopping-list/{id}/purchase` | Mark purchased |
| DELETE | `/api/shopping-list/purchased` | Clear purchased |

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications` | List notifications |

---

## Security

Sensitive configuration is stored in `backend/application-local.properties` (gitignored):

- Database password
- JWT secret
- Spoonacular API key

Use `application-local.properties.example` as a template.

---

## License

This project is private and proprietary.

---

## Contact

**Vageesh Kudutini Ramesh**

- [LinkedIn](https://www.linkedin.com/in/vageesh-kudutini-ramesh/)
- vageesh2001@gmail.com

---

**MealCraft** – Plan smarter. Eat better. 🍳
