# Use an official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar

# Expose port (optional, for documentation)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
