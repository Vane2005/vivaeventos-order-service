# Use a base image with Java 21 and Maven (eclipse-temurin, alpine variant)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies to leverage Docker layer caching
COPY pom.xml .
COPY .mvn .mvn
RUN mvn dependency:go-offline -B

# Copy the rest of the application code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Second stage: Create a smaller runtime image using eclipse-temurin (alpine variant)
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build stage
WORKDIR /app

WORKDIR /app

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
