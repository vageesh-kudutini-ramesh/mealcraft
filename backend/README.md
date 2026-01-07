# MealCraft Backend API

Backend REST API for MealCraft - Plan smarter. Eat better.

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17+
- **Security**: Spring Security with JWT authentication
- **Database**: PostgreSQL 15+
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Password Encryption**: BCrypt

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/mealcraft/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── model/          # Entity models
│   │   │   ├── repository/     # JPA repositories
│   │   │   ├── security/       # Security configuration
│   │   │   ├── service/        # Business logic
│   │   │   └── MealCraftApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## Setup Instructions

### Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** installed
3. **PostgreSQL 15+** installed and running
4. **pgAdmin 4** installed (for database management)

### Creating PostgreSQL Database using pgAdmin

Follow these steps to create the database using pgAdmin:

1. **Open pgAdmin 4**
   - Launch pgAdmin from your applications or start menu

2. **Connect to PostgreSQL Server**
   - In the left sidebar, expand "Servers"
   - Click on your PostgreSQL server (usually "PostgreSQL 15" or similar)
   - Enter your PostgreSQL master password if prompted

3. **Create New Database**
   - Right-click on "Databases" in the left sidebar
   - Select "Create" → "Database..."

4. **Configure Database**
   - In the "Create - Database" dialog:
     - **General Tab**:
       - **Database name**: `mealcraft_db`
       - **Owner**: Leave as default (usually `postgres`) or select your PostgreSQL user
     - **Definition Tab**:
       - **Encoding**: `UTF8` (default)
       - **Template**: `template0` (recommended for new databases)
     - Click **Save** to create the database

5. **Verify Database Creation**
   - You should see `mealcraft_db` listed under "Databases" in the left sidebar
   - Click on it to expand and view its contents

**Alternative: Using SQL Query Tool**
- Right-click on your PostgreSQL server → "Query Tool"
- Execute the following SQL command:
  ```sql
  CREATE DATABASE mealcraft_db;
  ```
- Press F5 or click the "Execute" button (▶) to run the query

### Configuration

1. **Update `src/main/resources/application.properties`**:

   **a) Database Connection** (if different from defaults):
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/mealcraft_db
   spring.datasource.username=postgres
   spring.datasource.password=your_postgres_password
   ```
   - Replace `your_postgres_password` with your actual PostgreSQL password
   - Default username is usually `postgres`
   - Default port is `5432` (change if your PostgreSQL uses a different port)

   **b) JWT Secret Key** (REQUIRED - Generate a secure key):
   
   **Option 1: Using PowerShell (Windows - Easiest Method)**
   
   Open PowerShell and run one of these commands:
   
   **Method A: Base64 encoded random bytes (Recommended)**
   ```powershell
   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
   ```
   This generates a secure 64-character base64-encoded random string.
   
   **Method B: Random alphanumeric string**
   ```powershell
   -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 64 | ForEach-Object {[char]$_})
   ```
   This generates a 64-character alphanumeric string.
   
   **Method C: Cryptographically secure random string**
   ```powershell
   $bytes = New-Object byte[] 48
   [System.Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
   [Convert]::ToBase64String($bytes)
   ```
   This generates a cryptographically secure random key (recommended for production).
   
   **Option 2: Using Online Generator (No Installation Required)**
   - Visit: https://www.allkeysgenerator.com/Random/Security-Encryption-Key
   - Select "256-bit" or "512-bit" key
   - Copy the generated key
   
   **Option 3: Using Java (If you have Java installed)**
   
   Create a file named `GenerateJWTSecret.java`:
   ```java
   import javax.crypto.KeyGenerator;
   import java.util.Base64;
   
   public class GenerateJWTSecret {
       public static void main(String[] args) throws Exception {
           KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
           keyGenerator.init(256);
           String secret = Base64.getEncoder().encodeToString(
               keyGenerator.generateKey().getEncoded()
           );
           System.out.println(secret);
       }
   }
   ```
   
   Then compile and run:
   ```powershell
   javac GenerateJWTSecret.java
   java GenerateJWTSecret
   ```
   
   **Option 4: Using Node.js (If you have Node.js installed for frontend)**
   ```powershell
   node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
   ```
   
   **Option 5: Install OpenSSL on Windows (If you prefer OpenSSL)**
   
   If you want to use OpenSSL on Windows:
   1. Download OpenSSL from: https://slproweb.com/products/Win32OpenSSL.html
   2. Install it (choose "Copy OpenSSL DLLs to" → "The OpenSSL binaries (/bin) directory")
   3. Add OpenSSL to PATH:
      - Open "Environment Variables" in Windows Settings
      - Add `C:\Program Files\OpenSSL-Win64\bin` to PATH (or your installation path)
      - Restart PowerShell
   4. Then use: `openssl rand -base64 64`
   
   **Option 6: Using Git Bash (If you have Git installed)**
   
   If you have Git for Windows installed, you can use Git Bash:
   ```bash
   openssl rand -base64 64
   ```
   (Git Bash includes OpenSSL)
   
   **After generating the key**, add it to `application.properties`:
   ```properties
   jwt.secret=YOUR_GENERATED_SECRET_KEY_HERE
   ```
   **Important**: 
   - The secret key must be at least 32 characters long
   - Keep this key secure and never commit it to version control
   - Use different keys for development and production environments
   - Example format: `jwt.secret=MealCraftSecretKeyForJWTTokenGeneration2024SecureAndLongEnoughForHS256Algorithm`

   **c) TheMealDB API Configuration** (for recipe suggestions):
   - **No setup required!** TheMealDB is completely free with no API key needed
   - The API is already configured in `application.properties`:
     ```properties
     themealdb.api.base-url=https://www.themealdb.com/api/json/v1/1
     ```
   - **Benefits**:
     - ✅ Completely free - no API key required
     - ✅ No rate limits or quotas
     - ✅ No account registration needed
     - ✅ Works immediately out of the box
   - **API Documentation**: https://www.themealdb.com/api.php

2. **Default Configuration Summary**:
   - Database URL: `jdbc:postgresql://localhost:5432/mealcraft_db`
   - Username: `postgres`
   - Password: `postgres` (change this!)
   - JWT Secret: Must be generated (see above)
   - JWT Expiration: `86400000` (24 hours in milliseconds)

### Running the Application

1. **Build the project**:
   ```bash
   cd backend
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the API**:
   - Base URL: `http://localhost:8080`
   - Health Check: `http://localhost:8080/api/health`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### User Profile
- `GET /api/users/profile` - Get current user profile
- `PUT /api/users/profile` - Update user profile

### Dashboard
- `GET /api/dashboard/stats` - Get dashboard statistics

### Pantry Management
- `GET /api/pantry` - Get all pantry items
- `GET /api/pantry/category/{category}` - Get items by category
- `GET /api/pantry/expiring` - Get expiring items
- `GET /api/pantry/expired` - Get expired items
- `GET /api/pantry/low-stock` - Get low-stock items
- `POST /api/pantry` - Create pantry item
- `PUT /api/pantry/{id}` - Update pantry item
- `DELETE /api/pantry/{id}` - Delete pantry item
- `DELETE /api/pantry/expired` - Delete all expired items
- `POST /api/pantry/{id}/use` - Mark item as used

### Recipe Management
- `POST /api/recipes/suggest` - Get recipe suggestions
- `GET /api/recipes/saved` - Get saved recipes
- `GET /api/recipes/saved/search?q={query}` - Search saved recipes
- `GET /api/recipes/saved/{id}` - Get saved recipe by ID
- `POST /api/recipes/saved` - Save recipe
- `PUT /api/recipes/saved/{id}` - Update saved recipe
- `DELETE /api/recipes/saved/{id}` - Delete saved recipe

### Meal Planning
- `GET /api/meal-plans/week?startDate={date}&endDate={date}` - Get weekly meal plan
- `GET /api/meal-plans/date?date={date}` - Get meal plan for date
- `POST /api/meal-plans` - Create meal plan
- `PUT /api/meal-plans/{id}` - Update meal plan
- `DELETE /api/meal-plans/{id}` - Delete meal plan
- `DELETE /api/meal-plans/date?date={date}` - Clear meal plan for date

### Shopping List
- `GET /api/shopping-list` - Get all shopping list items
- `GET /api/shopping-list/unpurchased` - Get unpurchased items
- `POST /api/shopping-list/generate?startDate={date}&endDate={date}` - Generate from meal plan
- `POST /api/shopping-list` - Create shopping list item
- `PUT /api/shopping-list/{id}` - Update shopping list item
- `POST /api/shopping-list/{id}/purchase` - Mark as purchased
- `DELETE /api/shopping-list/{id}` - Delete shopping list item
- `DELETE /api/shopping-list/purchased` - Clear purchased items

## Authentication

All endpoints except `/api/auth/**` and `/api/health` require JWT authentication.

Include JWT token in request header:
```
Authorization: Bearer <token>
```

## Database Schema

The application uses JPA/Hibernate with automatic schema generation (`spring.jpa.hibernate.ddl-auto=update`).

Main entities:
- **User** - User accounts
- **PantryItem** - Pantry inventory items
- **SavedRecipe** - User's saved recipes
- **MealPlan** - Weekly meal plans
- **ShoppingListItem** - Shopping list items

## External API Integration

### TheMealDB API

The application integrates with TheMealDB API for recipe suggestions based on pantry ingredients.

**About TheMealDB:**
- **Completely Free**: No API key, no registration, no rate limits
- **Open Source**: Community-driven recipe database
- **No Setup Required**: Works immediately out of the box
- **Comprehensive Database**: Hundreds of recipes from various cuisines

**How It Works:**

1. **No Configuration Needed**:
   - The API is pre-configured in `application.properties`
   - Base URL: `https://www.themealdb.com/api/json/v1/1`
   - No API key required

2. **Features**:
   - Search recipes by ingredient
   - Get full recipe details with ingredients and instructions
   - Recipe images included
   - Multiple cuisine types supported

3. **Implementation Details**:
   - Uses parallel API calls for optimal performance
   - Implements intelligent ingredient matching with normalization
   - Caches recipe data to reduce API calls
   - Finds recipes matching multiple pantry ingredients (intersection algorithm)

**API Endpoints Used:**
- `/filter.php?i={ingredient}` - Search recipes by ingredient
- `/lookup.php?i={recipeId}` - Get full recipe details

**API Documentation:**
- Full documentation: https://www.themealdb.com/api.php
- Test endpoints in browser: https://www.themealdb.com/api/json/v1/1/filter.php?i=chicken

**Performance Optimizations:**
- Parallel API calls using CompletableFuture
- In-memory caching (1 hour for recipes, 6 hours for ingredient searches)
- Smart filtering (only fetches top matching recipes)
- Ingredient name normalization for better matching

## Development

- Hot reload is enabled via Spring Boot DevTools
- Logging level: DEBUG (development), INFO (production)
- CORS enabled for frontend development (localhost:5173, localhost:3000)

## Troubleshooting

### Database Connection Issues

**Problem**: Cannot connect to PostgreSQL database

**Solutions**:
1. **Verify PostgreSQL is running**:
   - Check PostgreSQL service status in Services (Windows) or `systemctl status postgresql` (Linux)
   - Ensure PostgreSQL is listening on port 5432

2. **Check database credentials**:
   - Verify username and password in `application.properties`
   - Test connection using pgAdmin: Right-click server → "Properties" → "Connection"

3. **Verify database exists**:
   - Check in pgAdmin that `mealcraft_db` database exists
   - If missing, create it following the steps above

4. **Check firewall settings**:
   - Ensure port 5432 is not blocked by firewall
   - For remote connections, update `pg_hba.conf` file

5. **Connection string format**:
   - Ensure URL format is correct: `jdbc:postgresql://localhost:5432/mealcraft_db`
   - If using a different port, update the port number in the URL

### JWT Authentication Issues

**Problem**: JWT token validation fails or tokens are invalid

**Solutions**:
1. **Verify JWT secret key**:
   - Ensure the secret key in `application.properties` is at least 32 characters
   - The key must match between token generation and validation
   - If you changed the secret, all existing tokens will be invalid
   - **Windows users**: If `openssl` command doesn't work, use PowerShell method (see Configuration section)

2. **Check token expiration**:
   - Default expiration is 24 hours (86400000 milliseconds)
   - If tokens expire too quickly, increase `jwt.expiration` value

3. **Verify token format**:
   - Tokens should be sent as: `Authorization: Bearer <token>`
   - Ensure there's a space between "Bearer" and the token

**Problem**: Cannot generate JWT secret key on Windows

**Solutions**:
1. **Use PowerShell method** (Recommended - No installation needed):
   ```powershell
   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
   ```

2. **Use online generator**: https://www.allkeysgenerator.com/Random/Security-Encryption-Key

3. **Use Node.js** (if you have it installed for frontend):
   ```powershell
   node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
   ```

4. **Install OpenSSL for Windows** (see Configuration section for detailed steps)

### Common Build Errors

**Problem**: Maven build fails

**Solutions**:
1. **Clean and rebuild**:
   ```bash
   mvn clean install
   ```

2. **Check Java version**:
   ```bash
   java -version  # Should be 17 or higher
   ```

3. **Update Maven dependencies**:
   ```bash
   mvn dependency:resolve
   ```

4. **Check for port conflicts**:
   - Ensure port 8080 is not in use by another application
   - Change port in `application.properties` if needed: `server.port=8081`

### TheMealDB API Issues

**Problem**: Recipe suggestions not working

**Solutions**:
1. **Verify API configuration**:
   - Check that `themealdb.api.base-url` is set in `application.properties`
   - Default value: `https://www.themealdb.com/api/json/v1/1`
   - No API key needed - TheMealDB is completely free

2. **Check internet connection**:
   - Recipe suggestions require internet connection
   - Test API directly: https://www.themealdb.com/api/json/v1/1/filter.php?i=chicken
   - If this doesn't work, check your internet connection

3. **Verify API endpoint**:
   - Ensure `themealdb.api.base-url` is correct
   - No trailing slash should be present
   - Test the base URL in browser

4. **Check for empty pantry**:
   - Recipe suggestions require at least one pantry item
   - Ensure user has added items to pantry

5. **Review error logs**:
   - Check console output for specific error messages
   - Common issues:
     - Network timeout: Check internet connection
     - No recipes found: Try different ingredient names
     - Parsing errors: Usually indicates API response format changed (rare)

6. **Test API directly**:
   - Open browser and test: `https://www.themealdb.com/api/json/v1/1/filter.php?i=chicken`
   - Should return JSON with recipe data
   - If this fails, TheMealDB API might be temporarily down

**Note**: TheMealDB has no rate limits, so you can make unlimited API calls without any restrictions.

### Application Won't Start

**Problem**: Spring Boot application fails to start

**Solutions**:
1. **Check logs**:
   - Review console output for specific error messages
   - Check `logs/` directory if logging to file

2. **Verify database connection**:
   - Ensure PostgreSQL is running
   - Check database credentials

3. **Check for missing dependencies**:
   ```bash
   mvn dependency:tree
   ```

4. **Verify application.properties syntax**:
   - Ensure no syntax errors in property file
   - Check for missing quotes or incorrect property names

## Quick Reference

### Essential Configuration Properties

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mealcraft_db
spring.datasource.username=postgres
spring.datasource.password=your_password_here

# JWT Configuration (Generate a secure key - see Configuration section)
jwt.secret=YOUR_GENERATED_SECRET_KEY_HERE
jwt.expiration=86400000

# TheMealDB API (Completely free - no API key needed!)
themealdb.api.base-url=https://www.themealdb.com/api/json/v1/1

# Server Configuration
server.port=8080

# CORS Configuration
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

### Common Commands

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Check database connection (using psql)
psql -U postgres -d mealcraft_db -h localhost

# Generate JWT secret key
# PowerShell (Windows):
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# OpenSSL (macOS/Linux or if installed on Windows):
openssl rand -base64 64

# Node.js (if installed):
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

### Database Connection Test (pgAdmin)

1. Open pgAdmin → Right-click on `mealcraft_db` → "Query Tool"
2. Run: `SELECT version();`
3. If successful, database connection is working

## Production Considerations

1. **Security**:
   - Change JWT secret key to a strong, randomly generated value
   - Use strong database password (minimum 16 characters, mixed case, numbers, symbols)
   - Set `spring.jpa.hibernate.ddl-auto=validate` or `none` (never use `update` in production)
   - Configure proper CORS origins (remove localhost URLs)
   - Enable HTTPS/SSL for all connections

2. **Database**:
   - Use connection pooling (HikariCP is included by default)
   - Set up database backups
   - Configure read replicas for scaling (if needed)
   - Monitor database performance

3. **Application**:
   - Set appropriate logging levels (`INFO` or `WARN` for production)
   - Configure proper error handling and logging
   - Add API rate limiting
   - Implement request validation
   - Set up monitoring and alerting

4. **Infrastructure**:
   - Use environment variables for sensitive configuration
   - Set up CI/CD pipeline
   - Configure load balancing (if needed)
   - Set up health checks and monitoring

5. **Best Practices**:
   - Never commit secrets to version control
   - Use environment-specific configuration files
   - Implement proper error handling
   - Add comprehensive logging
   - Set up automated testing
   - Document API endpoints
   - Implement API versioning (if needed)


