# ═══════════════════════════════════════════════════════════
# GENERIC DOCKERFILE — Copy into EACH service folder
# Works for: eureka-server, api-gateway, auth-service,
#            timesheet-service, leave-service, admin-service
# ═══════════════════════════════════════════════════════════

# ── Stage 1: Build with Maven ──────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR (skip tests — run tests separately)
RUN mvn clean package -DskipTests

# ── Stage 2: Run with minimal JRE ──────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
# -Djava.security.egd speeds up startup on Linux containers
# by using /dev/urandom instead of /dev/random for entropy