# Build stage - run from repo root: docker build -f backend/Dockerfile .
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy backend files
COPY backend/pom.xml .
COPY backend/src src

# Build (skip tests for faster deploy)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built JAR
COPY --from=build /app/target/mealcraft-backend-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
